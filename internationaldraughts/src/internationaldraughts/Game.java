package internationaldraughts;

import static internationaldraughts.Board.ARROW;
import static internationaldraughts.Board.BLACK;
import static internationaldraughts.Board.BOARD;
import static internationaldraughts.Board.COLOR;
import static internationaldraughts.Board.GRID;
import static internationaldraughts.Board.LEVEL;
import static internationaldraughts.Board.MOVEABLE;
import static internationaldraughts.Board.WHITE;
import static internationaldraughts.Board.WINNER;
import static internationaldraughts.Board.paintTile;
import static internationaldraughts.Board.x;
import static internationaldraughts.Board.y;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.Cursor;
import java.util.Stack;

/**
 * Game
 * 
 * New game with color -> loop and logic.
 * Pieces are 'w' (white) and 'b' (black). Lowercase for man and uppercase for king.
 * Board is char[50] (begin position), empty tile = '_'.
 * A move is an ArrayList of Integers: captures & to
 * 
 * enum Direction -> move in 4 directions (x, y)
 * 
 * -turn: pieces, moves and maxCapture -> game over or move (player (mouse) or ai (MinMax)).
 * -move: animation of the move: move (jumps), promotion, captures. return color of new turn
 * 
 * ActionListener -> undo move. (player turn or gameover).
 * MouseListener (mousePressed) -> Player move -> piece + capture (2+) jumps + to.
 * 
 * @author Naardeze
 */

class Game extends Component implements ActionListener, MouseListener {
    final private static char W = 'w';
    final private static char B = 'b';
    
    final static String WB = W + "" + B;
    
    final static char[] MAN = WB.toCharArray();
    final static char[] KING = WB.toUpperCase().toCharArray();
    
    final static char EMPTY = '_';

    final static Image[][] PIECE = new Image[WB.length()][2];
    
    final private static Color ORANGE = Color.orange;
    final private static Color[] MOVE = {Color.yellow, Color.green};
    
    private static enum Direction {
        MIN_X_MIN_Y(-1, -1),
        PLUS_X_MIN_Y(1, -1),
        MIN_X_PLUS_Y(-1, 1),
        PLUS_X_PLUS_Y(1, 1);

        final int x;
        final int y;
        
        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean hasNext(int index) {
            int x = x(index) + this.x;
            int y = y(index) + this.y;

            return x >= 0 && x < GRID && y >= 0 && y < GRID;
        }

        int getNext(int index) {
            return (x(index) + x) / 2 + (y(index) + y) * (GRID / 2);
        }
        
        static Direction getDirection(int from, int to) {
            if (x(from) > x(to)) {
                if (from > to) {
                    return MIN_X_MIN_Y;
                } else {
                    return MIN_X_PLUS_Y;
                }
            } else {
                if (from > to) {
                    return PLUS_X_MIN_Y;
                } else {
                    return PLUS_X_PLUS_Y;
                }
            }
        }
        
    }
    
    final private Stack<String> boards = new Stack();
    
    final private int player;
    
    private char[] board = new char[BOARD.tile.length];
    private ArrayList<Integer> move = new ArrayList();
    
    private HashSet<Integer>[] pieces = new HashSet[WB.length()];
    private HashMap<Integer, ArrayList<Integer>[]> moves;
    private int maxCapture;
    
    final private static int NONE = -1;

    private int selected;
    
    Game(int player) {
        this.player = player;
        
        Arrays.fill(board, 0, board.length / 2 - GRID / 2, B);
        Arrays.fill(board, board.length / 2 - GRID / 2, board.length / 2 + GRID / 2, EMPTY);
        Arrays.fill(board, board.length / 2 + GRID / 2, board.length, W);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                turn(WHITE);
            }
        });
    }
    
    private void turn(int color) {
        pieces[WHITE] = new HashSet();
        pieces[BLACK] = new HashSet();
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] != EMPTY) {
                pieces[WB.indexOf(Character.toLowerCase(board[i]))].add(i);
            }
        }
        
        moves = new HashMap();
        maxCapture = 0;
        
        int opponent = 1 - color;
        
        for (int from : pieces[color]) {
            char piece = board[from];
            
            HashSet<ArrayList<Integer>> movesPiece = new HashSet();
            int maxCapturePiece = maxCapture;
            
            for (Direction[] horizontal : new Direction[][] {{Direction.MIN_X_MIN_Y, Direction.MIN_X_PLUS_Y}, {Direction.PLUS_X_MIN_Y, Direction.PLUS_X_PLUS_Y}}) {
                for (Direction vertical : horizontal) {
                    if (vertical.hasNext(from)) {
                        int step = vertical.getNext(from);
                        
                        if(board[step] == EMPTY && (piece == KING[color] || vertical == horizontal[color])) {
                            if (maxCapturePiece == 0) {
                                movesPiece.add(new ArrayList(Arrays.asList(new Integer[] {step})));
                            }

                            if (piece == KING[color] && vertical.hasNext(step)) {
                                do {
                                    step = vertical.getNext(step);

                                    if (maxCapturePiece == 0 && board[step] == EMPTY) {
                                        movesPiece.add(new ArrayList(Arrays.asList(new Integer[] {step})));
                                    }
                                } while (board[step] == EMPTY && vertical.hasNext(step));
                            }
                        }

                        if (pieces[opponent].contains(step) && vertical.hasNext(step)) {
                            int capture = step;

                            step = vertical.getNext(capture);
                            
                            if (board[step] == EMPTY) {
                                ArrayList<Integer> captureMove = new ArrayList(Arrays.asList(new Integer[] {capture, step}));

                                if (piece == KING[color] && vertical.hasNext(step)) {
                                    do {
                                        step = vertical.getNext(step);

                                        if (board[step] == EMPTY) {
                                            captureMove.add(step);
                                        }
                                    } while (board[step] == EMPTY && vertical.hasNext(step));
                                }

                                ArrayList<ArrayList<Integer>> captureMoves = new ArrayList(Arrays.asList(new ArrayList[] {captureMove}));

                                board[from] = EMPTY;

                                do {
                                    ArrayList<Integer> destination = captureMoves.remove(0);
                                    ArrayList<Integer> captured = new ArrayList();

                                    do {
                                        captured.add(destination.remove(0));
                                    } while (pieces[opponent].contains(destination.get(0)));

                                    if (captured.size() > maxCapturePiece) {
                                        movesPiece.clear();                                       
                                        maxCapturePiece++;
                                    }

                                    for (int to : destination) {
                                        if (captured.size() == maxCapturePiece) {
                                            ArrayList<Integer> move = new ArrayList(captured);
                                            
                                            move.add(to);
                                            movesPiece.add(move);
                                        }

                                        for (Direction diagonal : Direction.values()) {
                                            if (diagonal.hasNext(to)) {
                                                step = diagonal.getNext(to);                                                
                                                
                                                if (piece == KING[color] && !destination.contains(step)) {
                                                    while (board[step] == EMPTY && diagonal.hasNext(step)) {
                                                        step = diagonal.getNext(step);
                                                    }
                                                }

                                                if (pieces[opponent].contains(step) && !captured.contains(step) && diagonal.hasNext(step)) {
                                                    capture = step;
                                                    step = diagonal.getNext(capture);

                                                    if (board[step] == EMPTY) {
                                                        captureMove = new ArrayList(captured);
                                                        captureMove.addAll(Arrays.asList(new Integer[] {capture, step}));

                                                        if (piece == KING[color] && diagonal.hasNext(step)) {
                                                            do {
                                                                step = diagonal.getNext(step);
                                                                
                                                                if (board[step] == EMPTY) {
                                                                    captureMove.add(step);
                                                                }
                                                            } while (board[step] == EMPTY && diagonal.hasNext(step));
                                                        }

                                                        captureMoves.add(captureMove);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (!captureMoves.isEmpty());

                                board[from] = piece;
                            }
                        }
                    }
                }
            }
            
            if (!movesPiece.isEmpty()) {
                if (maxCapturePiece > maxCapture) {
                    moves.clear();
                    maxCapture = maxCapturePiece;
                }

                moves.put(from, movesPiece.toArray(new ArrayList[movesPiece.size()]));
            }
        }

        if (BOARD.isAncestorOf(this)) {
            if (moves.isEmpty()) {
                WINNER.setText(COLOR[opponent] + " is Winner");
            } else if (color == player) {
                selected = -1;
                addMouseListener(this);
                repaint();
            } else {                
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                        
                new Thread() {
                    @Override
                    public void run() {
                        ArrayList<Integer> move = MinMax.getAIMove(color, board, pieces, moves, maxCapture, LEVEL.getValue());
                
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        turn(move(color, move.remove(0), move));
                    }
                }.start();
            }
            
            if (moves.isEmpty() || color == player) {
                ARROW.setEnabled(true);
            }
        }
    }
    
    final private static int DELAY = 360;
    
    private int move(int color, int from, ArrayList<Integer> move) {
        this.move.clear();
        
        char piece = board[from];
        
        for (int i = 1; i < maxCapture; from = this.move.remove(i++)) {
            int capture = move.remove(0);
            Direction direction = Direction.getDirection(from, capture);
            int step = direction.getNext(capture);

            while (Math.abs(x(move.get(0)) - x(step)) != Math.abs(y(move.get(0)) - y(step))) {
                step = direction.getNext(step);
            }

            this.move.addAll(Arrays.asList(new Integer[] {capture, step}));

            board[from] = EMPTY;
            board[step] = piece;

            repaint();

            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {}
        }
        
        this.move.addAll(move);
        
        int to = this.move.get(maxCapture);
        
        if ((piece == W && to < GRID / 2) || (piece == B && to >= board.length - GRID / 2)) {
            piece = KING[color];
        }
        
        board[from] = EMPTY;
        board[to] = piece;
        
        repaint();
        
        try {
            Thread.sleep(DELAY);
        } catch (Exception ex) {}
        
        for (int i = 0; i < maxCapture; i++) {
            board[this.move.remove(0)] = EMPTY;
            
            repaint();
        
            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {}
        }
        
        return 1 - color;
    }
    
    @Override
    public void paint(Graphics g) {
        if (ARROW.isEnabled() && !moves.isEmpty()) {
            g.setColor(ORANGE);
            
            if (selected != NONE) {
                paintTile(g, BOARD.tile[selected]);
            } else if (MOVEABLE.isSelected()) {
                moves.keySet().forEach(hint -> paintTile(g, BOARD.tile[hint]));
            }
        }
        
        for (int index : move) {
            g.setColor(MOVE[(move.indexOf(index) + 1) / move.size()]);            
            paintTile(g, BOARD.tile[index]);
        }
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] != EMPTY) {
                g.drawImage(PIECE[WB.indexOf(Character.toLowerCase(board[i]))][(Character.toLowerCase(board[i]) + "" + Character.toUpperCase(board[i])).indexOf(board[i])], BOARD.tile[i].x, BOARD.tile[i].y, this);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (moves.isEmpty()) {
            WINNER.setText("");
        } else {
            removeMouseListener(this);
        }
        
        move.clear();
        board = boards.pop().toCharArray();
        
        ARROW.setEnabled(false);

        if (boards.isEmpty()) {
            ARROW.setVisible(false);
        }
        
        turn(player);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        for (int pressed = 0; pressed < BOARD.tile.length; pressed++) {
            if (BOARD.tile[pressed].contains(e.getPoint())) {
                if (selected != NONE && (board[pressed] == EMPTY || pressed == selected)) {
                    ArrayList<Integer> move = new ArrayList(this.move);
                    int step = move.remove(move.size() - 1);
                    
                    if (pressed != step && Math.abs(x(pressed) - x(step)) == Math.abs(y(pressed) - y(step))) {
                        Direction direction = Direction.getDirection(step, pressed);

                        step = direction.getNext(step);

                        if (board[selected] == KING[player]) {
                            while (step != pressed && (board[step] == EMPTY || step == selected)) {
                                step = direction.getNext(step);
                            }
                        }

                        if (pieces[1 - player].contains(step) && !move.contains(step)) {
                            move.add(step);
                            step = direction.getNext(step);

                            if (board[selected] == KING[player]) {
                                while (step != pressed && (board[step] == EMPTY || step == selected)) {
                                    step = direction.getNext(step);
                                }
                            }
                        } else if (move.size() < maxCapture || (board[selected] == W && direction.y == 1) || (board[selected] == B && direction.y == -1)) {
                            break;
                        }

                        if (step == pressed) {
                            move.add(pressed);

                            if (move.indexOf(pressed) == maxCapture) {
                                removeMouseListener(this);

                                ARROW.setEnabled(false);
                                
                                if (boards.isEmpty()) {
                                    ARROW.setVisible(true);
                                }
                                
                                boards.add(String.valueOf(board));

                                new Thread() {
                                    @Override
                                    public void run() {
                                        turn(move(player, selected, move));
                                    }
                                }.start();
                            } else {
                                this.move = move;

                                repaint();
                            }
                        }
                    } 
                } else if (board[pressed] != EMPTY) {
                    move.clear();
                    
                    if (moves.containsKey(pressed)) {
                        selected = pressed;
                        move.add(selected);
                    } else if (selected != NONE) {
                        selected = NONE;
                    }
                    
                    repaint();
                }
                
                break;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
    
}
