package fr.seynax.puissance4.impl.view;

import fr.seynax.puissance4.api.model.IGame;
import fr.seynax.puissance4.core.exception.ConnectException;
import fr.seynax.puissance4.api.model.IGridGameplay;
import fr.seynax.puissance4.core.Tokens;
import jcurses.system.CharColor;
import jcurses.system.Toolkit;

import java.util.*;

public class JCursesGameViewIA implements IGame
{
	// ATTRIBUTES

	private final IGridGameplay game;
	private String errorMessage;
	private boolean exitRequest;
	private boolean restartRequest;
	private final CharColor charColor;
	private int y;
	private int x;
	private int tokenPosition;
	private int lastTokenPosition;
	private boolean redraw;
	private boolean needAIChoice;
	private final List<String> messages;

	private final Map<Tokens, Integer> scores;

	// CONSTRUCTOR

	public JCursesGameViewIA(IGridGameplay game) {
		if (game == null) {
			throw new IllegalArgumentException("game ne peut �tre null");
		}
		this.game = game;
		Toolkit.init();
		this.y = 0;
		this.x = 0;
		tokenPosition = (int) (IGridGameplay.COLUMNS / 2);
		this.messages = new ArrayList<>();
		this.charColor = new CharColor(CharColor.BLACK, CharColor.WHITE);
		redraw = true;
		needAIChoice = true;
		scores = new HashMap<>();
	}

	// METHODS

	public void redraw() throws ConnectException {
		clearConsole();
		displayGrid();
		displayGameState();
		displayMessages();
		displayScore();
	}

	@Override
	public void play() throws InterruptedException
	{
		try {
			if (!game.isOver()) {
				if (redraw) {
					redraw();
					redraw = false;
				}

				if (needAIChoice) {
					this.tokenPosition = new Random().nextInt(IGridGameplay.COLUMNS-1);
					if(game.isAvailable(this.tokenPosition)) {
						needAIChoice = false;
						displayToken(this.tokenPosition, 2);
					}
				} else {
					putToken();
					if (!game.isOver()) {
						needAIChoice = true;
					}
				}
			} else {
				redraw();
				Thread.sleep(2000);
				restartRequest = true;
			}

			if (restartRequest) {
				game.init();
				restartRequest = false;
				redraw = true;
				tokenPosition = IGridGameplay.COLUMNS / 2;
				needAIChoice = true;
			}
		}catch (NumberFormatException e) {
			messages.addAll(Arrays.asList(("NumberFormatException : " + e.getMessage()).split("\n")));
			redraw = true;
			needAIChoice = true;
			this.tokenPosition = new Random().nextInt(IGridGameplay.COLUMNS-1);
		} catch (IllegalArgumentException e) {
			messages.addAll(Arrays.asList(("IllegalArgumentException : " + e.getMessage()).split("\n")));
			redraw = true;
			needAIChoice = true;
			this.tokenPosition = new Random().nextInt(IGridGameplay.COLUMNS-1);
		}  catch (ConnectException e) {
			messages.addAll(Arrays.asList(("ConnectException : " + e.getMessage()).split("\n")));
			redraw = true;
			needAIChoice = true;
			this.tokenPosition = new Random().nextInt(IGridGameplay.COLUMNS-1);
		}

		if (!exitRequest) {
			play();
		} else {
			clearConsole();
			printLn("EXIT_SUCCESS", CharColor.GREEN);
		}
	}
	
	// TOOLS


	private void putToken() throws ConnectException {
		if(game.isOver()) {
			return;
		}

		short lastForegroundColor = charColor.getForeground();
		var token = game.getCurrentPlayer();
		this.charColor.setForeground(token.getJcursesColor());
		for(int i = 0; i < IGridGameplay.ROWS; i ++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

			if(game.getToken(tokenPosition, (IGridGameplay.ROWS-1) - i) != null) {
				break;
			}

			clearPosition(this.tokenPosition * 6 + 3, Math.max((i - 1) * 3 + 4, 2));
			Toolkit.printString("" + token.getSymbol(), this.tokenPosition * 6 + 3, i * 3 + 4, this.charColor);
		}
		this.charColor.setForeground(lastForegroundColor);
		game.putToken(this.tokenPosition);
		tokenPosition = (int) (IGridGameplay.COLUMNS / 2);
	}

	private void displayGameState() {
		if (game.isOver()) {
			Tokens winner = game.getWinner();
			if (winner == null) {
				printLn("La partie s'est terminee sur un match nul.", CharColor.GREEN);
			} else {
				Integer score = scores.get(game.getWinner());
				if(score == null) {
					scores.put(game.getWinner(), 1);
				} else {
					scores.remove(game.getWinner());
					scores.put(game.getWinner(), score + 1);
				}

				print("La partie a ete remportee par ", CharColor.GREEN);
				print("" + winner.getSymbol(), winner.getJcursesColor());
				printLn(" !", CharColor.GREEN);
			}
		} else {
			print("C'est au tour de [");
			print("" + game.getCurrentPlayer().getSymbol(), game.getCurrentPlayer().getJcursesColor());
			printLn("] ! [0-" + (IGridGameplay.COLUMNS - 1) + "] : ");
		}
	}

	private void displayToken(int tokenPosition, int y) {
		var lastY = this.y;
		if(y >= 0) {
			this.y = y;
		}
		var token = game.getCurrentPlayer();
		clearPosition(lastTokenPosition * 6 + 3, this.y);
		printLn(" ".repeat(tokenPosition * 6 + 3) + token.getSymbol(), token.getJcursesColor());
		this.lastTokenPosition = tokenPosition;
		if(y >= 0) {
			this.y = lastY;
		}
	}

	private void displayScore() {
		var lastForeground = this.charColor.getForeground();
		this.charColor.setForeground(CharColor.YELLOW);

		Toolkit.printString("Scores", 6 * IGridGameplay.ROWS + 10, 1, this.charColor);
		int x = 0;
		for(var token : scores.keySet()) {
			var score = "" + this.scores.get(token);

			this.charColor.setForeground(token.getJcursesColor());
			Toolkit.printString("" + token.getSymbol(), 6 * IGridGameplay.ROWS + 10 + x, 2, this.charColor);
			this.charColor.setForeground(CharColor.YELLOW);
			Toolkit.printString(score, 6 * IGridGameplay.ROWS + 10 + x, 3, this.charColor);
			x += score.length() + 2;
		}

		this.charColor.setForeground(lastForeground);
	}

	private void displayGrid() throws ConnectException {
		printLn("");
		Tokens token;
		for (int x = 0; x < IGridGameplay.COLUMNS; x++) {
			print("   " + x + "  ");
		}
		printLn("");
		displayToken(tokenPosition, -1);

		for (int y = IGridGameplay.ROWS - 1; y >= 0; y--) {
			for (int x = 0; x < IGridGameplay.COLUMNS; x++) {
				print("|     ");
			}
			printLn("|");
			for (int x = 0; x < IGridGameplay.COLUMNS; x++) {
				print("|  ");
				token = game.getToken(x, y);
				if (token == null) {
					print(" ");
				} else {
					var color = token.getJcursesColor();
					for(var winPosition : game.getWinPositions()) {
						if(winPosition.getX() == x && winPosition.getY() == y) {
							color = CharColor.YELLOW;
						}
					}

					print("" + token.getSymbol(), color);
				}
				print("  ");
			}
			printLn("|");
			for (int x = 0; x < IGridGameplay.COLUMNS; x++) {
				print("|_____");
			}
			printLn("|");
		}
	}

	private void print(String message) {
		if(this.y > Toolkit.getScreenHeight()) {
			return;
		}
		print(message, this.charColor.getForeground());
	}

	private void print(String message, short textColor) {
		var lastForeground = this.charColor.getForeground();
		this.charColor.setForeground(textColor);
		Toolkit.printString(message, this.x, this.y, this.charColor);
		this.x+=message.length();
		this.charColor.setForeground(lastForeground);
	}

	private void printLn(String message) {
		printLn(message, this.charColor.getForeground());
	}

	private void printLn(String message, short textColor) {
		for (var line : message.split("\n")) {
			if(this.y > Toolkit.getScreenHeight()) {
				return;
			}
			var lastForeground = this.charColor.getForeground();
			this.charColor.setForeground(textColor);
			Toolkit.printString(line, this.x, this.y++, this.charColor);
			this.charColor.setForeground(lastForeground);
			this.x = 0;
		}
	}

	private void clearConsole() {
		Toolkit.clearScreen(this.charColor);
		this.y = 0;
	}

	public void displayMessages() {
		for(var message : messages) {
			printLn(">" + message, CharColor.RED);
		}
		if(y >= Toolkit.getScreenHeight()) {
			messages.clear();
		}
		for(int i = y; i < Toolkit.getScreenHeight(); i ++) {
			printLn(">", CharColor.RED);
		}
	}

	public void clearPosition(int x, int y) {
		Toolkit.printString(" ", x, y, this.charColor);
	}
}
