
from keras.models import Model, load_model, save_model
from keras.layers import Input, Conv2D, GlobalAveragePooling2D
from keras.layers import BatchNormalization, Add, Activation
from keras.optimizers import SGD

def build_iGo_model(filters, blocks, input_shape):

	inputs = Input(shape=input_shape)

	x = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(inputs)
	x = BatchNormalization()(x)

	for _ in range(blocks):
		y = Activation('relu')(x)
		y = BatchNormalization()(y)
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)

		y = Activation('relu')(y)
		y = BatchNormalization()(y)
		y = Conv2D(filters=filters, kernel_size=(3, 3), padding='same')(y)

		x = Add()([x,y])

	x = Activation('relu')(x)
	x = BatchNormalization()(x)

	policy = Conv2D(filters=1, kernel_size=(1, 1), padding='same', activation='sigmoid', name='policy')(x)

	territory = Conv2D(filters=1, kernel_size=(1, 1), padding='same', activation='tanh', name='territory')(x)

	value = Conv2D(filters=1, kernel_size=(1, 1), padding='same', activation='tanh', name='pre-value')(x)
	value = GlobalAveragePooling2D(name='value')(value)

	model = Model(inputs=inputs, outputs=[policy, territory, value])
	model.compile(
		loss={'policy':'categorical_crossentropy', 'territory':'binary_crossentropy', 'value':'mse'},
		loss_weights={'policy':0.45, 'territory':0.1, 'value':0.45},
		optimizer=SGD(learning_rate=2e-5, momentum=0.90))
	model.summary()
	return model

#model = build_iGo_model(64, 6, (9, 9, 6))
#exit()

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

class GameState:
	def __init__(self, board_size, board_state, player_to_move, policy, utility):
		self.board_size = board_size
		self.board_state = board_state
		self.player_to_move = player_to_move
		self.policy = policy
		self.utility = utility

class CompleteGameRecord:
	def __init__(self, board_size, array_of_game_states):
		""" from these pieces of information, i should:
				1) verify that the board size is correct and that all frames are the correct size
				2) determine the winner of the game and store that information as the game's outcome
				3) resolve the territory at the end of the game (by doing a random playout until there are no more moves?)
		"""

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
				frame_stack.append(GameState(board_size, board_state, player_to_move, policy, utility))
				if int(frame_buffer_size) == 0:
					print(board_size, line.split())
					complete_games.append(frame_stack)
					frame_stack = []
	return complete_games

test = parse('self-play data 20210115 vanilla mcts random rollouts')
print(len(test))
exit()





def create_dataset(source, samples, for_validation=False, puzzles_for_review=[]):
	""" Create the training dataset. """

	puzzles = np.zeros((samples, 9, 9, 9), dtype=np.int8)
	solutions = np.zeros((samples, 9, 9, 9), dtype=np.int8)

	s = 0
	for puzzle, solution in puzzles_for_review:
		puzzles[s], solutions[s] = create_random_homomorphism_in_place(puzzle, solution)
		s += 1

	print('resampled data records:', s)
	while s < samples:
		puzzle, solution = create_puzzle_solution_pair(source[int(npr()*len(source))], int(for_validation)+max(0, npr()))
		puzzles[s] = to_sparse(puzzle)
		solutions[s] = to_sparse(solution)
		s += 1

	return puzzles, solutions


""" Build a 2d model """
# model = build_2d_sudoku_model(64, (3, 3), 50)

""" Build a 3d model """
model = build_3d_sudoku_model(32, (3, 3, 3), 50)

""" Training loop """
batches = 10000
batch_size = 128
samples = batch_size * batches
resamples = samples // 5
best_pred_acc = 0
puzzles_for_review = []
herstory = {'predictive_acc': [], 'autoregressive_acc': []}

for e in range(1, 1000):
	puzzles, solutions = create_dataset(solved_puzzles, samples, puzzles_for_review=puzzles_for_review)
	history = model.fit(puzzles, solutions, batch_size=batch_size, epochs=1, verbose=1)

	predictions = model.predict(puzzles)
	puzzles_for_review = get_hardest_n_puzzles_by_mae(puzzles, solutions, predictions, resamples)

	puzzles, solutions = create_dataset(solved_puzzles, 1000, True, [])
	herstory['predictive_acc'] += [validate_predictions(puzzles, solutions, model)]

	puzzles, solutions = create_dataset(solved_puzzles, 100, True, [])
	herstory['autoregressive_acc'] += [autoregressive_validation(puzzles, solutions, model)]

	for key, value in history.history.items():
		if key in herstory:
			herstory[key] += value
		else:
			herstory[key] = value

	for key, value in herstory.items():
		print(key, ' '.join([str(x)[:6] for x in value]))

	if best_pred_acc < herstory['predictive_acc'][-1]:
		best_pred_acc = herstory['predictive_acc'][-1]
		save_model(model, 'sudoku model - one-shot_acc='+str(best_pred_acc)[:6], save_format='h5')
		print('model saved')
