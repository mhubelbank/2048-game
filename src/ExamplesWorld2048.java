import tester.Tester;

class ExamplesWorld2048 {
  // test the world functionality of Minesweeper
  void testGo(Tester t) {
    // create Minesweeper world - 20 x 20, 80 mines
    Board board = new Board();

    /*
    for (ArrayList<Cell> cells : board.cells) {
      for (Cell cell : cells) {
        cell.click(); // show the contents of each cell
      }
    }   
    */

    board.bigBang(IConstants.GAMEBOARD_WIDTH, IConstants.GAMEBOARD_HEIGHT, 1/260);
  }
}
