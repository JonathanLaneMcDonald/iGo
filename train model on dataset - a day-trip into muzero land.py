
import numpy as np
from numpy.random import random, choice

from keras.models import Model, load_model, save_model
from keras.layers import Input, Conv2D, GlobalAveragePooling2D, Concatenate, Reshape
from keras.layers import BatchNormalization, Add, Activation, Flatten, Dense
from keras.optimizers import SGD

def build_representation_model(filters, blocks):
	inputs = Input(shape=(9,9,10))
	x = inputs

	for b in range(blocks):
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(x)
		y = BatchNormalization()(y)
		y = Activation('relu')(y)

		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)
		y = BatchNormalization()(y)

		if b:		x = Add()([x,y])
		else:		x = y

		x = Activation('relu')(x)

	x = Conv2D(filters=1, kernel_size=(1, 1), padding='same')(x)
	x = BatchNormalization()(x)
	hidden_state = Activation('relu')(x)

	model = Model(inputs=inputs, outputs=hidden_state, name='repr_model')
	model.summary()
	return model

def build_dynamics_model(filters, blocks):
	hidden_state = Input(shape=(9,9,1))
	next_action = Input(shape=(9,9,1))

	x = Concatenate()([hidden_state, next_action])

	for b in range(blocks):
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(x)
		y = BatchNormalization()(y)
		y = Activation('relu')(y)

		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)
		y = BatchNormalization()(y)

		if b:		x = Add()([x,y])
		else:		x = y

		x = Activation('relu')(x)

	x = Conv2D(filters=1, kernel_size=(1, 1), padding='same')(x)
	x = BatchNormalization()(x)
	next_hidden_state = Activation('relu')(x)

	model = Model(inputs=[hidden_state, next_action], outputs=next_hidden_state, name='dyna_model')
	model.summary()
	return model

def build_prediction_model(filters, blocks, policy_space):
	hidden_state = Input(shape=(9,9,1))
	x = hidden_state

	for b in range(blocks):
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(x)
		y = BatchNormalization()(y)
		y = Activation('relu')(y)

		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)
		y = BatchNormalization()(y)

		if b:		x = Add()([x,y])
		else:		x = y

		x = Activation('relu')(x)

	policy_head = Conv2D(filters=2, kernel_size=(1, 1), padding='same')(x)
	policy_head = BatchNormalization()(policy_head)
	policy_head = Activation('relu')(policy_head)
	policy_head = Flatten()(policy_head)
	policy = Dense(policy_space, activation='softmax', name='policy')(policy_head)

	value_head = Conv2D(filters=1, kernel_size=(1,1), padding='same')(x)
	value_head = BatchNormalization()(value_head)
	value_head = Activation('relu')(value_head)
	value_head = Flatten()(value_head)
	value_head = Dense(256, activation='relu')(value_head)
	value = Dense(1, activation='tanh', name='value')(value_head)

	model = Model(inputs=hidden_state, outputs=[policy, value], name='policy_model')
	model.summary()
	return model

def build_fused_muzero_model(filters, aux_blocks, main_blocks, policy_space):

	repr_model = build_representation_model(filters, aux_blocks)
	dyna_model = build_dynamics_model(filters, aux_blocks)
	policy_model = build_prediction_model(filters, main_blocks, policy_space)

	game_state = Input(shape=(9,9,10))
	hidden_state_1 = repr_model(game_state)
	policy_1, value_1 = policy_model(hidden_state_1)

	future_action_1 = Input(shape=(9,9,1))
	hidden_state_2 = dyna_model([hidden_state_1, future_action_1])
	policy_2, value_2 = policy_model(hidden_state_2)

	future_action_2 = Input(shape=(9,9,1))
	hidden_state_3 = dyna_model([hidden_state_2, future_action_2])
	policy_3, value_3 = policy_model(hidden_state_3)

	policy_1 = Activation('softmax', name='policy_1')(policy_1)
	policy_2 = Activation('softmax', name='policy_2')(policy_2)
	policy_3 = Activation('softmax', name='policy_3')(policy_3)

	value_1 = Activation('tanh', name='value_1')(value_1)
	value_2 = Activation('tanh', name='value_2')(value_2)
	value_3 = Activation('tanh', name='value_3')(value_3)

	model = Model(inputs=[game_state, future_action_1, future_action_2], outputs=[policy_1, policy_2, policy_3, value_1, value_2, value_3])
	model.compile(
		loss={	'policy_1':'sparse_categorical_crossentropy', 'value_1':'mse',
				'policy_2':'sparse_categorical_crossentropy', 'value_2':'mse',
				'policy_3':'sparse_categorical_crossentropy', 'value_3':'mse'},
		loss_weights={	'policy_1':1.00, 'value_1':1.00,
						'policy_2':0.30, 'value_2':0.30,
						'policy_3':0.20, 'value_3':0.20},
		optimizer=SGD(learning_rate=0.01, momentum=0.90),
		metrics=['accuracy'])
	model.summary()
	return model

def build_muzero_inference_model(other_model):

	game_state = Input(shape=(9,9,10))

	hidden_state = other_model.get_layer(name='repr_model')(game_state)
	policy, value = other_model.get_layer(name='policy_model')(hidden_state)

	policy = Activation('linear', name='policy')(policy)
	value = Activation('linear', name='value')(value)

	model = Model(inputs=game_state, outputs=[policy, value])
	model.compile(
		loss={'policy':'sparse_categorical_crossentropy', 'value':'mse'},
		loss_weights={'policy':0.5, 'value':0.5},
		optimizer=SGD(learning_rate=0.01, momentum=0.90),
		metrics=['accuracy'])
	model.summary()
	return model

def build_iGo_model(filters, blocks, input_shape, policy_space):
	"""
	model architecture:
		inputs:
			channel 1:	location is on board
			channel 2:	current state (black)
			channel 3:	current state (white)
			channel 4:	previous state (black)
			channel 5:	previous state (white)
			channel 6:	player to move

		outputs:
			policy
			value
	"""

	inputs = Input(shape=input_shape)
	x = inputs

	for b in range(blocks):
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(x)
		y = BatchNormalization()(y)
		y = Activation('relu')(y)

		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)
		y = BatchNormalization()(y)

		if b:		x = Add()([x,y])
		else:		x = y

		x = Activation('relu')(x)

	policy_head = Conv2D(filters=2, kernel_size=(1, 1), padding='same')(x)
	policy_head = BatchNormalization()(policy_head)
	policy_head = Activation('relu')(policy_head)
	policy_head = Flatten()(policy_head)
	policy = Dense(policy_space, activation='softmax', name='policy')(policy_head)

	value_head = Conv2D(filters=1, kernel_size=(1,1), padding='same')(x)
	value_head = BatchNormalization()(value_head)
	value_head = Activation('relu')(value_head)
	value_head = Flatten()(value_head)
	value_head = Dense(256, activation='relu')(value_head)
	value = Dense(1, activation='tanh', name='value')(value_head)

	model = Model(inputs=inputs, outputs=[policy, value])
	model.compile(
		loss={'policy':'sparse_categorical_crossentropy', 'value':'mse'},
		loss_weights={'policy':0.5, 'value':0.5},
		optimizer=SGD(learning_rate=0.01, momentum=0.90),
		metrics=['accuracy'])
	model.summary()
	return model


class GameState:
	def __init__(self, board_state, player_to_move, policy, utility):
		self.board_state = board_state
		self.player_to_move = player_to_move
		self.policy = policy
		self.utility = utility


class CompleteGameRecord:
	def __init__(self, board_size, array_of_game_states):
		""" from these pieces of information, i should:
				1) verify that the board size is correct and that all frames are the correct size
				2) determine the winner of the game and store that information as the game's outcome
					(this can be determined as there is either a 1 or a 0 for the likelihood)
					(do not accept a game unless the outcome is either 1 or 0 as this enables me to...)
				3) resolve the territory at the end of the game (by doing a random playout until there are no more moves)
					(but i think i'm going to systematically not care about territory just yet)
			after these 3 steps, i'll have six input channels:
				location is on board
				positions for each player for current and previous board positions
				player to move
			and i'll have the output objectives:
				policy for player to move (per move)
				end-game territory (global)
				game outcome (global)
		"""

		self.valid_game_record = True

		self.board_size = board_size
		self.board_area = self.board_size**2

		self.game_states = array_of_game_states
		for state in self.game_states:
			if len(state.board_state) != self.board_area:
				self.valid_game_record = False

		self.game_outcome = 0
		if self.game_states[-1].utility >= 0.95 and self.game_states[-1].player_to_move == "W":
			self.game_outcome = -1
		elif self.game_states[-1].utility <= 0.05 and self.game_states[-1].player_to_move == "B":
			self.game_outcome = -1
		elif self.game_states[-1].utility >= 0.95 and self.game_states[-1].player_to_move == "B":
			self.game_outcome = 1
		elif self.game_states[-1].utility <= 0.05 and self.game_states[-1].player_to_move == "W":
			self.game_outcome = 1

		if self.game_outcome == 0:
			self.valid_game_record = False

	def isValid(self):
		return self.valid_game_record


def parse(filename):
	complete_games = []
	frame_stack = []
	board_size = 0
	with open(filename,'r') as f:
		for line in f:
			if len(line.split()) == 5:
				frame_buffer_size, board_state, player_to_move, policy, utility = line.split()
				if len(frame_stack) == 0:
					board_size = int(len(board_state)**0.5)

				if policy == 'pass':
					policy = board_size**2

				frame_stack.append(GameState(board_state, player_to_move, int(policy), float(utility)))
				if int(frame_buffer_size) == 0:
					game_record = CompleteGameRecord(board_size, frame_stack)
					if game_record.isValid():
						#print(board_size, line.split())
						complete_games.append(game_record)

					frame_stack = []

	return complete_games


def create_dataset(games, samples, maxSize):
	features = np.zeros((samples, maxSize, maxSize, 10), dtype=np.int8)
	policy = np.zeros((samples, 1), dtype=np.int16)
	value = np.zeros((samples, 1), dtype=np.int8)

	s = 0
	while s < samples:
		selected_game = games[int(random()*len(games))]
		move_number = int(random()*len(selected_game.game_states))

		for r in range(maxSize):
			for c in range(maxSize):
				if r < selected_game.board_size and c < selected_game.board_size:
					features[s][r][c][0] = 1

				for t in range(4):
					if move_number - t >= 0:
						if r < selected_game.board_size and c < selected_game.board_size:
							if selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "B":
								features[s][r][c][1+(2*t+0)] = 1
							elif selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "W":
								features[s][r][c][1+(2*t+1)] = 1

				if selected_game.game_states[move_number].player_to_move == "B":
					features[s][r][c][9] = 1

		# set up the policy
		policy[s][0] = selected_game.game_states[move_number].policy

		# set up the value
		value[s][0] = selected_game.game_outcome

		s += 1

	return features, policy, value

def create_zero_dataset(games, samples, maxSize):
	game_state = np.zeros((samples, maxSize, maxSize, 10), dtype=np.int8)
	future_action_1 = np.zeros((samples, maxSize, maxSize, 1), dtype=np.int8)
	future_action_2 = np.zeros((samples, maxSize, maxSize, 1), dtype=np.int8)
	policy_1 = np.zeros((samples, 1), dtype=np.uint8)
	policy_2 = np.zeros((samples, 1), dtype=np.uint8)
	policy_3 = np.zeros((samples, 1), dtype=np.uint8)
	value_1 = np.zeros((samples, 1), dtype=np.int8)
	value_2 = np.zeros((samples, 1), dtype=np.int8)
	value_3 = np.zeros((samples, 1), dtype=np.int8)

	s = 0
	while s < samples:
		selected_game = games[int(random()*len(games))]
		move_number = int(random()*len(selected_game.game_states))

		for r in range(maxSize):
			for c in range(maxSize):
				if r < selected_game.board_size and c < selected_game.board_size:
					game_state[s][r][c][0] = 1

				for t in range(4):
					if move_number - t >= 0:
						if r < selected_game.board_size and c < selected_game.board_size:
							if selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "B":
								game_state[s][r][c][1+(2*t+0)] = 1
							elif selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "W":
								game_state[s][r][c][1+(2*t+1)] = 1

				if selected_game.game_states[move_number].player_to_move == "B":
					game_state[s][r][c][9] = 1

		if move_number+1 < len(selected_game.game_states) and selected_game.game_states[move_number+1].policy != maxSize**2:
			p = selected_game.game_states[move_number+1].policy
			r = p//maxSize
			c = p%maxSize
			future_action_1[s][r][c][0] = 1

		if move_number+2 < len(selected_game.game_states) and selected_game.game_states[move_number+2].policy != maxSize**2:
			p = selected_game.game_states[move_number+2].policy
			r = p//maxSize
			c = p%maxSize
			future_action_2[s][r][c][0] = 1

		if move_number+0 < len(selected_game.game_states):
			policy_1[s][0] = selected_game.game_states[move_number+0].policy

		if move_number+1 < len(selected_game.game_states):
			policy_2[s][0] = selected_game.game_states[move_number+1].policy

		if move_number+2 < len(selected_game.game_states):
			policy_3[s][0] = selected_game.game_states[move_number+2].policy

		value_1[s][0] = selected_game.game_outcome
		value_2[s][0] = selected_game.game_outcome
		value_3[s][0] = selected_game.game_outcome

		s += 1

	return game_state, future_action_1, future_action_2, policy_1, policy_2, policy_3, value_1, value_2, value_3

import time

def speed_test(model, games, maxSize, iterations, batchSize):
	start_time = time.time()
	for i in range(1, iterations):
		features, policy, value = create_dataset(games, batchSize, maxSize)
		policy, value = model.predict(features)
		if i % 10 == 0:
			print(time.time()-start_time, i*batchSize, (i*batchSize)/(time.time()-start_time))

""" Model Params """
maxSize = 9

""" Build a 2d model """
#model = build_iGo_model(128, 20, (maxSize, maxSize, 10), 1 + maxSize**2)
model = build_fused_muzero_model(128, 6, 10, 1 + maxSize**2)

""" Load Training Data """
games = parse('self-play data 20210114-8 vanilla mcts random rollouts')
print(len(games),'games loaded into training set')

""" Speed Test """
#speed_test(model, games, maxSize, 1000000, 32)

""" Training loop """
batches = 128
batch_size = 1024
samples = batch_size * batches

for e in range(1, 1000):

	#features, policy, value = create_dataset(games, samples, maxSize)
	#history = model.fit(features, [policy, value], batch_size=batch_size, epochs=1, verbose=1)

	game_state, future_action_1, future_action_2, policy_1, policy_2, policy_3, value_1, value_2, value_3 = create_zero_dataset(games, samples, maxSize)
	history = model.fit([game_state, future_action_1, future_action_2], [policy_1, policy_2, policy_3, value_1, value_2, value_3], batch_size=batch_size, epochs=1, verbose=1)

	features, policy, value = create_dataset(games, 10000, maxSize)
	inference_model = build_muzero_inference_model(model)
	inference_model.evaluate(features, [policy, value])




