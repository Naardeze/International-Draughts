package internationaldraughts;

import static internationaldraughts.Board.GRID;
import static internationaldraughts.Game.EMPTY;
import static internationaldraughts.Game.KING;
import static internationaldraughts.Game.MAN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

/**
 * MinMax
 * 
 * AI move, using basic minimax with alfa beta pruning.
 * 1 Level = 2 moves (alfa & beta).
 * If search depth is reached continues while board contains captures.
 * HashMap for lookup -> no dubble calculations.
 * Move generating looks like Game.turn (don't invent the wheel twice).
 * A 'move' bitboard contains all captures and destinations
 * 
 * enum Node -> alfa beta
 * enum Diagonal -> move in 4 directions (bitboards)
 * 
 * @author Naardeze
 */

class MinMax extends HashMap<String, Integer> {
    final private static int ALFA = Integer.MAX_VALUE;
    final private static int BETA = Integer.MIN_VALUE;

    private static enum Node {
        MAX {
            @Override
            int toAlfaBeta(int alfa, int value) {
                return Math.max(alfa, value);
            }

            @Override
            int valueOf(int value) {
                return value;
            }
        },
        MIN {
            @Override
            int toAlfaBeta(int beta, int value) {
                return Math.min(beta, value);
            }

            @Override
            int valueOf(int value) {
                return -value;
            }
        };
        
        abstract int toAlfaBeta(int alfaBeta, int value);
        abstract int valueOf(int value);
    }
    
    final private static int COLUMN = GRID / 2;
    final private static int ROW = GRID - 1;
    
    private static enum Diagonal {
        MIN_X_MIN_Y(COLUMN, 0, -COLUMN) {
            @Override
            long getLine(int index, long occupied, long from) {
                long diagonal = MIN_PLUS[COLUMN - 1 - index % COLUMN + index / COLUMN % 2 + index / GRID];

                return diagonal & (occupied ^ Long.reverse(Long.reverse(diagonal & occupied) - Long.reverse(from)));
            }
        }, 
        PLUS_X_MIN_Y(COLUMN - 1, 0, -COLUMN + 1) {
            @Override
            long getLine(int index, long occupied, long from) {
                long diagonal = PLUS_MIN[index % COLUMN + index / GRID];

                return diagonal & (occupied ^ Long.reverse(Long.reverse(diagonal & occupied) - Long.reverse(from)));
            }
        }, 
        MIN_X_PLUS_Y(COLUMN, ROW, COLUMN) {
            @Override
            long getLine(int index, long occupied, long from) {
                long diagonal = PLUS_MIN[index % COLUMN + index / GRID];

                return diagonal & (occupied ^ ((diagonal & occupied) - from));
            }
        }, 
        PLUS_X_PLUS_Y(COLUMN - 1, ROW, COLUMN + 1) {
            @Override
            long getLine(int index, long occupied, long from) {
                long diagonal = MIN_PLUS[COLUMN - 1 - index % COLUMN + index / COLUMN % 2 + index / GRID];

                return diagonal & (occupied ^ ((diagonal & occupied) - from));
            }
        };

        final int column;
        final int row;
        final int step;

        Diagonal(int column, int row, int step) {
            this.column = column;
            this.row = row;
            this.step = step;
        }

        boolean hasNext(int index) {
            return index % GRID != column && index / COLUMN != row;
        }

        long getNext(int index) {
            return 1l << index + step - index / COLUMN % 2;
        }

        abstract long getLine(int index, long occupied, long from);

        final private static long[] MIN_PLUS = new long[GRID];
        final private static long[] PLUS_MIN = new long[GRID - 1];

        static {
            for (int i = 0; i < MIN_PLUS.length; i++) {
                MIN_PLUS[i] = 0l;

                for (int j = COLUMN - 1 - Math.min(i, COLUMN - 1) + i / COLUMN * COLUMN + Math.max(0, i - COLUMN) * GRID; Long.bitCount(MIN_PLUS[i]) < 1 + (Math.min(i, COLUMN - 1) - Math.max(0, i - COLUMN)) * 2; j += COLUMN + 1 - j / COLUMN % 2) {
                    MIN_PLUS[i] ^= 1l << j;
                }
            }

            for (int i = 0; i < PLUS_MIN.length; i++) {
                PLUS_MIN[i] = 0l;

                for (int j = Math.min(i, COLUMN - 1) + Math.max(0, i - (COLUMN - 1)) * GRID; Long.bitCount(PLUS_MIN[i]) < 2 + (Math.min(i, COLUMN - 1) - Math.max(0, i - (COLUMN - 1))) * 2; j += COLUMN - j / COLUMN % 2) {
                    PLUS_MIN[i] ^= 1l << j;
                }
            }
        }
    }

    private static long middle = 0l;

    static {
        for (int i = COLUMN; i < ROW * COLUMN; i++) {
            if (i % GRID != COLUMN - 1 && i % GRID != COLUMN) {
                middle ^= 1l << i;
            }
        }
    }
    
    final private Node node;
    final private int color;
    
    MinMax(Node node, int color) {
        this.node = node;
        this.color = color;
    }
    
    private int valueOf(char[] board, long turn, long opponent, MinMax minMax, int[] alfaBeta, int value, int depth) {
        HashMap<Integer, HashSet<Long>> moves = new HashMap();
        int maxCapture = 0;
    
        for (long empty = ~(turn ^ opponent), pieces = turn; pieces != 0l; pieces ^= Long.lowestOneBit(pieces)) {
            int from = Long.numberOfTrailingZeros(pieces);
            boolean isKing = board[from] == KING[color];
            
            HashSet<Long> movesPiece = new HashSet();
            int maxCapturePiece = maxCapture;

            for (Diagonal[] horizontal : new Diagonal[][] {{Diagonal.MIN_X_MIN_Y, Diagonal.MIN_X_PLUS_Y}, {Diagonal.PLUS_X_MIN_Y, Diagonal.PLUS_X_PLUS_Y}}) {
                for (Diagonal vertical : horizontal) {
                    if (vertical.hasNext(from)) {
                        long move = vertical.getNext(from);

                        if (isKing && (move & middle & empty) == move) {
                            move = vertical.getLine(from, ~empty, move);
                        }
                       
                        long capture = move & opponent;
                        
                        if ((capture & middle) != 0l) {
                            long step = vertical.getNext(Long.numberOfTrailingZeros(capture));
                            
                            if ((step & empty) == step) {
                                if (isKing && (step & middle) == step) {
                                    step = vertical.getLine(from, ~empty, step) & empty;
                                }
                                
                                ArrayList<Long> captureMoves = new ArrayList(Arrays.asList(new Long[] {capture ^ step}));
                                
                                empty ^= 1l << from;
                                
                                do {
                                    move = captureMoves.remove(0);

                                    long captures = move & opponent;
                
                                    if (Long.bitCount(captures) >= maxCapturePiece) {
                                        if (Long.bitCount(captures) > maxCapturePiece) {
                                            movesPiece.clear();
                                            maxCapturePiece++;
                                        }
                                        
                                        movesPiece.add(move);
                                    }
                                    
                                    for (long destination = move ^ captures; destination != 0l; destination ^= Long.lowestOneBit(destination)) {
                                        int to = Long.numberOfTrailingZeros(destination);

                                        for (Diagonal diagonal : Diagonal.values()) {
                                            if (diagonal.hasNext(to)) {
                                                step = diagonal.getNext(to);
                                                
                                                if (isKing && (step & middle & empty) == step) {
                                                    step = diagonal.getLine(to, ~empty, step);
                                                }

                                                if ((step & move) == 0l) {
                                                    capture = step & opponent;

                                                    if ((capture & middle) != 0l) {
                                                        step = diagonal.getNext(Long.numberOfTrailingZeros(capture));

                                                        if ((step & empty) == step) {
                                                            if (isKing && (step & middle) == step) {
                                                                step = diagonal.getLine(to, ~empty, step) & empty;
                                                            }

                                                            captureMoves.add(captures ^ capture ^ step);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (!captureMoves.isEmpty());
                                
                                empty ^= 1l << from;
                            }
                        }
            
                        if (maxCapturePiece == 0 && (isKing || vertical == horizontal[color])) {
                            move &= empty;

                            if (move != 0l) {
                                movesPiece.add(move);
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
                
                moves.put(from, movesPiece);
            }
        }
        
        if (moves.isEmpty()) {
            return alfaBeta[node.ordinal()];
        } else if (depth > 0) {
            depth -= node.ordinal();
        } else if (maxCapture == 0) {
            return value;
        }
        
        value += node.valueOf(maxCapture);
        
        for (int from : moves.keySet()) {
            char piece = board[from];
            
            board[from] = EMPTY;
            
            for (long move : moves.get(from)) {
                long captures = move & opponent;
                ArrayList<Integer> captured = new ArrayList();
                
                for (long l = captures; l != 0l; l ^= Long.lowestOneBit(l)) {
                    captured.add(Long.numberOfTrailingZeros(l));
                }
                
                for (long destination = move ^ captures; destination != 0l; destination ^= Long.lowestOneBit(destination)) {
                    int to = Long.numberOfTrailingZeros(destination);
                    String key = String.valueOf(getBoard(color, board.clone(), piece, captured, to));
                    
                    if (!containsKey(key)) {
                        put(key, minMax.valueOf(key.toCharArray(), opponent ^ captures, turn ^ (1l << from ^ 1l << to), this, alfaBeta.clone(), value, depth));
                    }
                    
                    alfaBeta[node.ordinal()] = node.toAlfaBeta(alfaBeta[node.ordinal()], get(key));
                    
                    if (alfaBeta[Node.MAX.ordinal()] >= alfaBeta[Node.MIN.ordinal()]) {
                        return alfaBeta[node.ordinal()];
                    }
                }
            }
            
            board[from] = piece;
        }
        
        return alfaBeta[node.ordinal()];
    }
    
    private static char[] getBoard(int color, char[] board, char piece, ArrayList<Integer> captured, int to) {
        if (piece == MAN[color] && to / COLUMN == color * ROW) {
            piece = KING[color];
        }
        
        board[to] = piece;
        
        captured.forEach(capture -> board[capture] = EMPTY);
        
        return board;
    }
    
    static ArrayList<Integer> getAIMove(int ai, char[] board, HashSet<Integer>[] pieces, HashMap<Integer, HashSet<ArrayList<Integer>>> moves, int maxCapture, int level) {
        int player = 1 - ai;
        
        long turn =  0l;
        long opponent =  0l;
        
        for (int index : pieces[ai]) {
            turn ^= 1l << index;
        }
        
        
        for (int index : pieces[player]) {
            opponent ^= 1l << index;
        }
        
        MinMax minMaxMin = new MinMax(Node.MIN, player);
        MinMax minMaxMax = new MinMax(Node.MAX, ai);
        
        ArrayList<ArrayList<Integer>> alfaMoves = new ArrayList();
        int max = BETA;
        
        for (int from : moves.keySet()) {
            char piece = board[from];
            
            board[from] = EMPTY;
            
            for (ArrayList<Integer> move : moves.get(from)) {
                int to = move.remove(maxCapture);
                long captures = 0l;
                
                for (int capture : move) {
                    captures ^= 1l << capture;
                }
                
                int min = minMaxMin.valueOf(getBoard(ai, board.clone(), piece, move, to), opponent ^ captures, turn ^ (1l << from ^ 1l << to), minMaxMax, new int[] {BETA, ALFA}, maxCapture, level);
                
                if (min >= max) {
                    if (min > max) {
                        alfaMoves.clear();
                        max = min;
                    }
                    
                    move.add(0, from);
                    move.add(to);
                    
                    alfaMoves.add(move);
                }
            }

            board[from] = piece;
        }
        
        return alfaMoves.get((int) (Math.random() * alfaMoves.size()));
    }
    
}
