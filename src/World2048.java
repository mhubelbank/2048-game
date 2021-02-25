// Authored by Mara Hubelbank in May-June 2020 out of early pandemic boredom.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to store constant values
interface IConstants {
  int N_CELLS = 4;

  int CELL_SIDE = 80;
  int CELL_GAP = 10;
  int BOARD_SIDE = CELL_SIDE * 4 + CELL_GAP * 5;

  int LABEL_HEIGHT = 2 * CELL_SIDE - 2 * CELL_GAP;
  int GAMEBOARD_WIDTH = BOARD_SIDE + CELL_SIDE;
  int GAMEBOARD_HEIGHT = BOARD_SIDE + CELL_SIDE + LABEL_HEIGHT;

  Color BACKGROUND_COLOR = new Color(251, 248, 241);
  Color EMPTY_COLOR = new Color(206, 192, 181);
  Color BORDER_COLOR = new Color(188, 173, 161);
  Color TEXT_24_COLOR = new Color(104, 95, 87); // for 2, 4, and title
  Color TEXT_COLOR = new Color(249, 240, 236);
  Color SCORE_LABEL_COLOR = new Color(239, 224, 201);

  @SuppressWarnings("serial")
  HashMap<Integer, Color> TILE_COLORS = new HashMap<Integer, Color>() {
    {
      put(2, new Color(238, 228, 217));
      put(4, new Color(239, 224, 201));
      put(8, new Color(240, 178, 122));
      put(16, new Color(235, 142, 83));
      put(32, new Color(242, 126, 93));
      put(64, new Color(237, 88, 56));
      put(128, new Color(241, 216, 106));
      put(256, new Color(241, 208, 76));
      put(512, new Color(249, 202, 88));
      put(1024, new Color(237, 197, 63));
      put(2048, new Color(251, 197, 45));
    }
  };
}

// to represent a game tile with a number, or an empty cell (num -1)
class Tile {
  int num;

  boolean spawning;
  int moving;

  // for creating an empty cell
  Tile() {
    this.num = -1;
    this.spawning = false;
    this.moving = 0;
  }

  // for creating a new numbered tile
  Tile(int num) {
    this.num = num;
    this.spawning = true;
    this.moving = 0;
  }

  // to return the image representation of this cell (tile or empty)
  WorldImage draw() {

    // empty cell
    if (this.num == -1) {
      return new RectangleImage(IConstants.CELL_SIDE, IConstants.CELL_SIDE, OutlineMode.SOLID,
          IConstants.EMPTY_COLOR);
    }

    // tile cell
    else {
      int digits = this.numDigits();
      Color textColor = (this.num <= 4) ? IConstants.TEXT_24_COLOR : IConstants.TEXT_COLOR;
      Color tileColor = IConstants.TILE_COLORS.get(this.num);

      int textSize = (digits < 3) ? (IConstants.CELL_SIDE * 3 / 4)
          : ((IConstants.CELL_SIDE * 3) / (digits * 2));
      int textOffset = (digits == 1) ? 0 : textSize / 20;

      if (this.spawning) {
        RectangleImage tileSpawn = new RectangleImage(IConstants.CELL_SIDE * 7 / 8,
            IConstants.CELL_SIDE * 7 / 8, OutlineMode.SOLID, tileColor);
        TextImage textSpawn = new TextImage(Integer.toString(this.num), textSize * 7 / 8,
            FontStyle.BOLD, textColor);
        this.spawning = !this.spawning; // not spawning anymore
        return new OverlayOffsetImage(textSpawn, textOffset * 7 / 8, 0, tileSpawn);
      }

      else {
        RectangleImage tile = new RectangleImage(IConstants.CELL_SIDE, IConstants.CELL_SIDE,
            OutlineMode.SOLID, tileColor);
        TextImage text = new TextImage(Integer.toString(this.num), textSize, FontStyle.BOLD,
            textColor);

        return new OverlayOffsetImage(text, textOffset, 0, tile);
      }
    }
  }

  // count the number of digits in this tile's number
  int numDigits() {
    int count = 0;
    int num = this.num;

    while (num != 0) {
      num /= 10;
      count++;
    }

    return count;
  }

}

// to represent a 2048 game board with 16 tiles
class Board extends World {
  Random rand;

  ArrayList<ArrayList<Tile>> cells; // tiles and empty cells
  ArrayList<Posn> emptyPosns; // positions that don't have tiles

  int maxTile;
  int score;
  int best;

  boolean gameOver;
  boolean win;

  // initial board; spawn two 2-tiles in random positions
  Board() {
    this.rand = new Random();
    this.initializeBoard();
    this.best = 0;
  }

  // initialize the tiles and list of empty positions
  void initializeBoard() {
    this.cells = new ArrayList<ArrayList<Tile>>(4); // 4 row capacity
    this.emptyPosns = new ArrayList<Posn>(16); // 16 cell capacity

    // add new empty tiles to this board
    for (int i = 0; i < IConstants.N_CELLS; i++) {
      ArrayList<Tile> row = new ArrayList<Tile>(IConstants.N_CELLS);
      for (int j = 0; j < IConstants.N_CELLS; j++) {
        Posn posn = new Posn(i, j);
        this.emptyPosns.add(posn);

        Tile tile = new Tile();
        row.add(tile); // all tiles have num -1 (they're empty)
      }
      this.cells.add(row);
    }

    // add two random tiles of value 2 onto this board
    this.spawnTile(2);
    this.spawnTile(2);

    this.maxTile = 2;
    this.score = 0;

    this.gameOver = false;
    this.win = false;
  }

  // to spawn a tile with the given value at a random position
  // where value is either 2 or 4
  void spawnTile(int value) {
    // get a random position of an empty cell
    int index = this.rand.nextInt(this.emptyPosns.size());

    Tile tile = new Tile(value);

    Posn posn = this.emptyPosns.remove(index); // this posn isn't empty now

    // get the row at the given y-pos, set the cell at the given x-pos to new tile
    this.cells.get(posn.x).set(posn.y, tile);
  }

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
   *                                 WORLD METHODS                                     *
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

  // to render the scene for the 2048 game
  @Override
  public WorldScene makeScene() {
    WorldScene scene = this.getEmptyScene();

    // outer game board
    RectangleImage gameImage = new RectangleImage(IConstants.GAMEBOARD_WIDTH,
        IConstants.GAMEBOARD_HEIGHT, OutlineMode.SOLID, IConstants.BACKGROUND_COLOR);

    scene.placeImageXY(gameImage, IConstants.GAMEBOARD_WIDTH / 2, IConstants.GAMEBOARD_HEIGHT / 2);

    // game grid
    RectangleImage boardImage = new RectangleImage(IConstants.BOARD_SIDE, IConstants.BOARD_SIDE,
        OutlineMode.SOLID, IConstants.BORDER_COLOR);

    scene.placeImageXY(boardImage, IConstants.GAMEBOARD_WIDTH / 2,
        (IConstants.CELL_SIDE / 2) + IConstants.LABEL_HEIGHT + (IConstants.BOARD_SIDE / 2));

    // header (title/description/score boxes)
    scene = this.drawHeader(scene);

    // grid of cells
    for (int i = 0; i < IConstants.N_CELLS; i++) {
      for (int j = 0; j < IConstants.N_CELLS; j++) {
        Tile cell = this.cells.get(i).get(j);
        WorldImage cellDrawn = cell.draw();
        int boardX = this.jToX(j);
        int boardY = this.iToY(i);
        scene.placeImageXY(cellDrawn, boardX, boardY);
      }
    }

    // if they've gotten the last tile, end the game (win)
    if (this.maxTile == 2048) {
      this.endGame(true);
    }

    // if the board is filled
    else if (this.emptyPosns.size() == 0) {
      // check if they have moves left to make
      if (!this.movesLeft()) {
        this.endGame(false); // if they don't, end the game
      }
    }

    // if the game is over, display a game-over message
    if (this.gameOver) {
      WorldImage gameOverBox = new RectangleImage(IConstants.BOARD_SIDE * 3 / 4,
          IConstants.BOARD_SIDE / 4, OutlineMode.SOLID, IConstants.TEXT_24_COLOR);

      TextImage textTop = new TextImage((this.win) ? "You won!" : "Game over!",
          IConstants.CELL_SIDE / 3, FontStyle.BOLD, IConstants.BACKGROUND_COLOR);
      TextImage textBottom = new TextImage("Press enter to play again :)", IConstants.CELL_SIDE / 4,
          FontStyle.BOLD, IConstants.BACKGROUND_COLOR);

      gameOverBox = new OverlayOffsetImage(textTop, 0, IConstants.CELL_GAP * 3 / 2, gameOverBox);
      gameOverBox = new OverlayOffsetImage(textBottom, 0, -IConstants.CELL_GAP * 3 / 2,
          gameOverBox);

      scene.placeImageXY(gameOverBox, IConstants.GAMEBOARD_WIDTH / 2,
          (IConstants.CELL_SIDE / 2) + IConstants.LABEL_HEIGHT + (IConstants.BOARD_SIDE / 2));
    }

    return scene;
  }

  // draw the title, description, and score boxes onto the given scene
  WorldScene drawHeader(WorldScene scene) {
    TextImage title = new TextImage(Integer.toString(2048), IConstants.CELL_SIDE * 9 / 10,
        FontStyle.BOLD, IConstants.TEXT_24_COLOR);

    scene.placeImageXY(title, IConstants.CELL_SIDE * 3 / 2, IConstants.CELL_SIDE * 8 / 9);

    TextImage description = new TextImage("Join the numbers and get to the 2048 tile!",
        IConstants.CELL_SIDE * 18 / 80, FontStyle.BOLD, IConstants.TEXT_24_COLOR);

    scene.placeImageXY(description, IConstants.BOARD_SIDE / 2 + IConstants.CELL_GAP * 7 / 2,
        IConstants.CELL_SIDE * 2 - IConstants.CELL_GAP * 2);

    WorldImage scoreBestBox = new RectangleImage(IConstants.CELL_SIDE, IConstants.CELL_SIDE / 2,
        OutlineMode.SOLID, IConstants.BORDER_COLOR);

    TextImage scoreLabel = new TextImage("SCORE", (IConstants.CELL_SIDE / 5), FontStyle.BOLD,
        IConstants.SCORE_LABEL_COLOR);

    TextImage scoreText = new TextImage(Integer.toString(this.score), (IConstants.CELL_SIDE / 5),
        FontStyle.BOLD, IConstants.TEXT_COLOR);

    OverlayOffsetImage scoreBox = new OverlayOffsetImage(scoreLabel, 0, IConstants.CELL_GAP,
        scoreBestBox);
    scoreBox = new OverlayOffsetImage(scoreText, 0, -IConstants.CELL_GAP, scoreBox);

    scene.placeImageXY(scoreBox,
        IConstants.GAMEBOARD_WIDTH - IConstants.CELL_SIDE * 2 - 2 * IConstants.CELL_GAP,
        IConstants.CELL_SIDE - IConstants.CELL_GAP / 2);

    TextImage bestLabel = new TextImage("BEST", (IConstants.CELL_SIDE / 5), FontStyle.BOLD,
        IConstants.SCORE_LABEL_COLOR);

    TextImage bestText = new TextImage(Integer.toString(this.best), (IConstants.CELL_SIDE / 5),
        FontStyle.BOLD, IConstants.TEXT_COLOR);

    OverlayOffsetImage bestBox = new OverlayOffsetImage(bestLabel, 0, IConstants.CELL_GAP,
        scoreBestBox);
    bestBox = new OverlayOffsetImage(bestText, 0, -IConstants.CELL_GAP, bestBox);

    scene.placeImageXY(bestBox,
        IConstants.GAMEBOARD_WIDTH - IConstants.CELL_SIDE - IConstants.CELL_GAP,
        IConstants.CELL_SIDE - IConstants.CELL_GAP / 2);

    return scene;
  }

  // to convert a column index to its pixel x-position on the game board
  int jToX(int j) {
    return (IConstants.CELL_SIDE + IConstants.CELL_GAP)
        + (j * (IConstants.CELL_SIDE + IConstants.CELL_GAP));
  }

  // to convert a row index to its pixel y-position on the game board
  int iToY(int i) {
    return (IConstants.CELL_SIDE / 2 + IConstants.CELL_GAP)
        + (i * (IConstants.CELL_SIDE + IConstants.CELL_GAP)) + (IConstants.CELL_SIDE / 2)
        + IConstants.LABEL_HEIGHT;
  }

  // to move the tiles in the specified direction on a key press
  // and spawn a new tile if at least one event happens (movement/combination)
  @Override
  public void onKeyEvent(String key) {

    // if the game is over, restart or end world
    if (this.gameOver) {
      if (key.equals("enter") || key.equals("up") || key.equals("down") || key.equals("left")
          || key.equals("right")) {
        this.initializeBoard();
      }
      else {
        this.endOfWorld(":(");
      }
    }

    // still in game => handle movement
    else {
      int events = 0;

      // move the tiles based on the given key
      switch (key) {
      case "up":
        events = this.moveTilesUp();
        break;
      case "down":
        events = this.moveTilesDown();
        break;
      case "left":
        events = this.moveTilesLeft();
        break;
      case "right":
        events = this.moveTilesRight();
        break;
      default:
        break;
      }

      // update high score if necessary
      if (this.score > this.best) {
        this.best = this.score;
      }

      // if an event happened, spawn a tile
      if (events > 0) {
        this.spawnTile(this.rand.nextInt(2) * 2 + 2);
      }
    }

  }

  // end the game with the given win condition
  void endGame(boolean win) {
    this.gameOver = true;
    this.win = win;
  }

  // to determine whether the player still has moves to play
  boolean movesLeft() {
    boolean movesLeft = false;
    for (int i = 0; i < IConstants.N_CELLS - 1; i++) {
      for (int j = 0; j < IConstants.N_CELLS - 1; j++) {
        Tile current = this.cells.get(i).get(j);
        Tile right = this.cells.get(i).get(j + 1);
        Tile below = this.cells.get(i + 1).get(j);
        movesLeft = movesLeft || (current.num == right.num) || (current.num == below.num);

        // if on the second-to-last column, check the last cell with the cell below it
        if (j == IConstants.N_CELLS - 2) {
          Tile rightRight = this.cells.get(i).get(j + 1);
          Tile rightRightBelow = this.cells.get(i + 1).get(j + 1);
          movesLeft = movesLeft || rightRight.num == rightRightBelow.num;
        }

        // if on the last row, check the last row's neighbors
        if (i == IConstants.N_CELLS - 2) {
          Tile currentBelow = this.cells.get(i + 1).get(j);
          Tile rightBelow = this.cells.get(i + 1).get(j + 1);
          movesLeft = movesLeft || (currentBelow.num == rightBelow.num);
        }
      }
    }
    return movesLeft;
  }

  // return the row index of the highest empty cell that a tile at the given
  // position can move to, or its own row index if it can't move
  int highestEmptyRowInCol(int currRow, int thisCol) {
    int row = currRow; // assume nothing above this tile is empty
    for (int i = currRow - 1; i >= 0; i--) {
      Tile above = this.cells.get(i).get(thisCol);
      if (above.num != -1) {
        return row; // we've reached another tile -- return top-most so far
      }
      else {
        row = i; // new top-most found
      }
    }
    return row;
  }

  // move all the tiles on the board up as much as possible,
  // and combine neighboring tiles when necessary
  // return the number of events (moves or combinations) that happen
  int moveTilesUp() {

    int events = 0;

    // go across columns
    for (int j = 0; j < IConstants.N_CELLS; j++) {

      // go down this column, starting at second cell
      for (int i = 1; i < IConstants.N_CELLS; i++) {

        Tile tile = this.cells.get(i).get(j);

        // if this cell contains a tile
        if (tile.num != -1) {

          // the cell directly above this one
          Tile above = this.cells.get(i - 1).get(j);

          // if this tile can't move, this method returns the same i-value
          int newRow = this.highestEmptyRowInCol(i, j);

          // if this cell can move, move it to the highest empty cell
          if (i != newRow) {
            this.cells.get(newRow).set(j, new Tile(tile.num)); // add this tile to new spot
            this.emptyPosns.remove(new Posn(newRow, j)); // its new spot isn't empty now

            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            // if this isn't the top-most tile, check it again for combination
            if (newRow != 0) {
              i = newRow - 1; // sub 1 to cancel out i++
            }

            events++; // an event happened (we can spawn a new tile)
          }

          // if this tile can't move, but can be combined, combine it with above tile
          else if (tile.num == above.num) {
            int combinedNum = tile.num + above.num;

            // if this is the biggest tile, replace maxTile
            if (combinedNum > this.maxTile) {
              this.maxTile = combinedNum;
            }

            this.score += combinedNum; // add the new combined tile to the score

            this.cells.get(i - 1).set(j, new Tile(combinedNum)); // combination replaces above tile
            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            events++; // an event happened (we can spawn a new tile)
          }

          // other case: top number != bottom number -> do nothing
        }

      }
    }

    return events;
  }

  // return the row index of the lowest empty cell that a tile at the given
  // position can move to, or its own row index if it can't move
  int lowestEmptyRowInCol(int currRow, int thisCol) {
    int row = currRow; // assume nothing below this tile is empty
    for (int i = currRow + 1; i < IConstants.N_CELLS; i++) {
      Tile below = this.cells.get(i).get(thisCol);
      if (below.num != -1) {
        return row; // we've reached another tile -- return bottom-most so far
      }
      else {
        row = i; // new bottom-most found
      }
    }
    return row;
  }

  // move all the tiles on the board down as much as possible,
  // and combine neighboring tiles when necessary
  // return the number of events (moves or combinations) that happen
  int moveTilesDown() {

    int events = 0;

    // go across columns
    for (int j = 0; j < IConstants.N_CELLS; j++) {

      // go up this column, starting at second-to-last cell
      for (int i = IConstants.N_CELLS - 2; i >= 0; i--) {

        Tile tile = this.cells.get(i).get(j);

        // if this cell contains a tile
        if (tile.num != -1) {

          // the cell directly below this one
          Tile below = this.cells.get(i + 1).get(j);

          // if this tile can't move, this method returns the same i-value
          int newRow = this.lowestEmptyRowInCol(i, j);

          // if this cell can move, move it to the highest empty cell
          if (i != newRow) {
            this.cells.get(newRow).set(j, new Tile(tile.num)); // add this tile to new spot
            this.emptyPosns.remove(new Posn(newRow, j)); // its new spot isn't empty now

            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            // if this isn't the bottom-most tile, check it again for combination
            if (newRow != IConstants.N_CELLS - 1) {
              i = newRow + 1; // add 1 to cancel out i--
            }

            events++; // an event happened (we can spawn a new tile)
          }

          // if this tile can't move, but can be combined, combine it with below tile
          else if (tile.num == below.num) {
            int combinedNum = tile.num + below.num;

            // if this is the biggest tile, replace maxTile
            if (combinedNum > this.maxTile) {
              this.maxTile = combinedNum;
            }

            this.score += combinedNum; // add the new combined tile to the score

            this.cells.get(i + 1).set(j, new Tile(combinedNum)); // combination replaces below tile
            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            events++; // an event happened (we can spawn a new tile)
          }

          // other case: top number != bottom number -> do nothing
        }

      }
    }

    return events;
  }

  // return the column index of the leftmost empty cell that a tile at the given
  // position can move to, or its own column index if it can't move
  int leftmostEmptyColInRow(int thisRow, int currCol) {
    int col = currCol; // assume nothing to the left of this tile is empty
    for (int j = currCol - 1; j >= 0; j--) {
      Tile left = this.cells.get(thisRow).get(j);
      if (left.num != -1) {
        return col; // we've reached another tile -- return left-most so far
      }
      else {
        col = j; // new left-most found
      }
    }
    return col;
  }

  // move all the tiles on the board left as much as possible,
  // and combine neighboring tiles when necessary
  // return the number of events (moves or combinations) that happen
  int moveTilesLeft() {

    int events = 0;

    // go down rows
    for (int i = 0; i < IConstants.N_CELLS; i++) {

      // go across this row, starting at second cell
      for (int j = 1; j < IConstants.N_CELLS; j++) {

        Tile tile = this.cells.get(i).get(j);

        // if this cell contains a tile
        if (tile.num != -1) {

          // the cell directly to the left of this one
          Tile above = this.cells.get(i).get(j - 1);

          // if this tile can't move, this method returns the same i-value
          int newCol = this.leftmostEmptyColInRow(i, j);

          // if this cell can move, move it to the leftmost empty cell
          if (j != newCol) {
            this.cells.get(i).set(newCol, new Tile(tile.num)); // add this tile to new spot
            this.emptyPosns.remove(new Posn(i, newCol)); // its new spot isn't empty now

            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            // if this isn't the left-most tile, check it again for combination
            if (newCol != 0) {
              j = newCol - 1; // sub 1 to cancel out j++
            }

            events++; // an event happened (we can spawn a new tile)
          }

          // if this tile can't move, but can be combined, combine it with above tile
          else if (tile.num == above.num) {
            int combinedNum = tile.num + above.num;

            // if this is the biggest tile, replace maxTile
            if (combinedNum > this.maxTile) {
              this.maxTile = combinedNum;
            }

            this.score += combinedNum; // add the new combined tile to the score

            this.cells.get(i).set(j - 1, new Tile(combinedNum)); // combination replaces left tile
            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            events++; // an event happened (we can spawn a new tile)
          }

          // other case: top number != bottom number -> do nothing
        }

      }
    }

    return events;
  }

  // return the column index of the rightmost empty cell that a tile at the given
  // position can move to, or its own column index if it can't move
  int rightmostEmptyColInRow(int thisRow, int currCol) {
    int col = currCol; // assume nothing to the right of this tile is empty
    for (int j = currCol + 1; j < IConstants.N_CELLS; j++) {
      Tile left = this.cells.get(thisRow).get(j);
      if (left.num != -1) {
        return col; // we've reached another tile -- return left-most so far
      }
      else {
        col = j; // new left-most found
      }
    }
    return col;
  }

  // move all the tiles on the board right as much as possible,
  // and combine neighboring tiles when necessary
  // return the number of events (moves or combinations) that happen
  int moveTilesRight() {

    int events = 0;

    // go down rows
    for (int i = 0; i < IConstants.N_CELLS; i++) {

      // go backwards across this row, starting at second-to-last cell
      for (int j = IConstants.N_CELLS - 2; j >= 0; j--) {

        Tile tile = this.cells.get(i).get(j);

        // if this cell contains a tile
        if (tile.num != -1) {

          // the cell directly to the right of this one
          Tile above = this.cells.get(i).get(j + 1);

          // if this tile can't move, this method returns the same i-value
          int newCol = this.rightmostEmptyColInRow(i, j);

          // if this cell can move, move it to the leftmost empty cell
          if (j != newCol) {
            this.cells.get(i).set(newCol, new Tile(tile.num)); // add this tile to new spot
            this.emptyPosns.remove(new Posn(i, newCol)); // its new spot isn't empty now

            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            // if this isn't the right-most tile, check it again for combination
            if (newCol != IConstants.N_CELLS - 1) {
              j = newCol + 1; // add 1 to cancel out j--
            }

            events++; // an event happened (we can spawn a new tile)
          }

          // if this tile can't move, but can be combined, combine it with above tile
          else if (tile.num == above.num) {
            int combinedNum = tile.num + above.num;

            // if this is the biggest tile, replace maxTile
            if (combinedNum > this.maxTile) {
              this.maxTile = combinedNum;
            }

            this.score += combinedNum; // add the new combined tile to the score

            this.cells.get(i).set(j + 1, new Tile(combinedNum)); // combination replaces left tile
            this.cells.get(i).set(j, new Tile()); // add empty tile to its old spot
            this.emptyPosns.add(new Posn(i, j)); // its old position is empty now

            events++; // an event happened (we can spawn a new tile)
          }

          // other case: top number != bottom number -> do nothing
        }

      }
    }

    return events;
  }
}