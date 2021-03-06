
import numpy as np
from numpy.random import random, choice

from tensorflow.keras.models import Model, load_model, save_model
from tensorflow.keras.layers import Input, Conv2D, GlobalAveragePooling2D
from tensorflow.keras.layers import BatchNormalization, Add, Activation, Flatten, Dense
from tensorflow.keras.optimizers import SGD

def build_iGo_model(filters, blocks, input_shape, policy_space):

	"""
	model architecture:
		inputs:
			channel 0:	        whether or not the location is on the board (to accommodate different board sizes)
			channel 1 + 2k+0:   black stones for kth position on the game state stack
			channel 1 + 2k+1:   white stones for kth position on the game state stack
			channel n-1:	    player to move

		outputs:
			policy
			value
	"""

	inputs = Input(shape=input_shape)

	x = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(inputs)
	x = BatchNormalization()(x)
	x = Activation('relu')(x)

	for _ in range(blocks):
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(x)
		y = BatchNormalization()(y)
		y = Activation('relu')(y)

		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)
		y = BatchNormalization()(y)

		x = Add()([x,y])

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
		return self.valid_game_record and len(self.game_states)
	
	def gameAsPolicyString(self, first_k_moves):
		return ' '.join([str(x.policy) for x in self.game_states[:first_k_moves]])


def parse(filename):
	complete_games = []
	frame_stack = []
	board_size = 0

	frames_loaded = 0
	frames_discharged = 0
	unique_games = set()
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
					while len(game_record.game_states) and not (0.05 <= game_record.game_states[-1].utility <= 0.95):
						frames_discharged += 1
						game_record.game_states.pop()
					if game_record.isValid():
						unique_games.add(game_record.gameAsPolicyString(20))
						frames_loaded += len(game_record.game_states)
						complete_games.append(game_record)

					frame_stack = []

	print(len(complete_games),'complete games loaded\n', frames_loaded, 'frames loaded\n', frames_discharged, 'frames discharged\n', len(unique_games),'unique games played')

	return complete_games


def create_dataset(games, samples, maxSize, moveHistory):
	features = np.zeros((samples, maxSize, maxSize, 2 + 2*moveHistory), dtype=np.int8)
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

				for t in range(moveHistory):
					if move_number - t >= 0:
						if r < selected_game.board_size and c < selected_game.board_size:
							if selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "B":
								features[s][r][c][1+(2*t+0)] = 1
							elif selected_game.game_states[move_number-t].board_state[r*selected_game.board_size+c] == "W":
								features[s][r][c][1+(2*t+1)] = 1

				if selected_game.game_states[move_number].player_to_move == "B":
					features[s][r][c][2 + 2*moveHistory - 1] = 1

		# set up the policy
		policy[s][0] = selected_game.game_states[move_number].policy

		# set up the value
		value[s][0] = selected_game.game_outcome

		s += 1

	return features, policy, value

import time

def speed_test(model, games, maxSize, iterations, batchSize, moveHistory):
	start_time = time.time()
	for i in range(1, iterations):
		features, policy, value = create_dataset(games, batchSize, maxSize, moveHistory)
		policy, value = model.predict(features)
		if i % 10 == 0:
			print(time.time()-start_time, i*batchSize, (i*batchSize)/(time.time()-start_time))

""" Model Params """
moveHistory = 1

filters = 32
residualBlocks = 4
maxBoardSize = 9

""" Build a 2d model """
inputChannels = 2 + 2*moveHistory
model = build_iGo_model(filters, residualBlocks, (maxBoardSize, maxBoardSize, inputChannels), 1 + maxBoardSize**2)

""" Load Training Data """
games = parse('self-play data 7,8,9 20210114-9 vanilla mcts random rollouts')
print(len(games),'games loaded into training set')

""" Speed Test """
#speed_test(model, games, maxBoardSize, 1000000, 64, moveHistory)

""" Training loop """
batches = 128
batch_size = 1024
samples = batch_size * batches

for e in range(1, 1000):
	features, policy, value = create_dataset(games, samples, maxBoardSize, moveHistory)

	az_history = model.fit(features, [policy, value], batch_size=batch_size, epochs=1, verbose=1).history
	model.save('az model loss='+str(az_history['loss'][-1])[:8]+' pacc='+str(az_history['policy_accuracy'][-1])[:8]+' vacc='+str(az_history['value_accuracy'][-1])[:8]+'.h5')

