package internationaldraughts;

import static internationaldraughts.Game.PIECE;
import static internationaldraughts.Game.WB;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Board (main)
 * 
 * JFrame with at board (10x10), on which all games are played, in the center.
 * The colors are of int: WHITE (0) and BLACK (1).
 * 
 * Board (JPanel) has 50 dark tiles (rectangles) on a light background.
 * When the board is sized the tiles and piece images are also sized (x and y (static)).
 * static paintTile paints a filled tile (board or game).
 * 
 * main handles the setup and layout of the frame.
 * 
 * the menubar contains 2 menus:
 * 1: Game -> new game white or black
 * 2: AI -> level 1-5 -> 1 level = 2 moves
 * 
 * Extra option are:
 * -ARROW -> undo move
 * -WINNER -> winning color at end of game
 -MOVEABLE -> moveable pieces (orange)
 * -rotate -> rotate the board
 * 
 * @author Naardeze
 */

class Board extends JPanel implements ActionListener {
    final static int WHITE = 0;
    final static int BLACK = 1;
    
    final static String[] COLOR = {"White", "Black"};
    
    final static int GRID = 10;
    
    final private static Color LIGHT = Color.white;
    final private static Color DARK = Color.lightGray;
    
    final static JSlider LEVEL = new JSlider(1, 5);
    
    final static Board BOARD = new Board();
    
    final static JButton ARROW = new JButton("\ud83e\udc44");
    final static JLabel WINNER = new JLabel();
    final static JCheckBox MOVEABLE = new JCheckBox();
    
    private static Game game = new Game(WHITE);
    
    final Rectangle[] tile = new Rectangle[GRID * GRID / 2];
    
    private Board() {
        super(new BorderLayout());
        
        setBackground(LIGHT);
        setForeground(DARK);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth() / GRID;
                int height = getHeight() / GRID;
        
                for (int i = 0; i < tile.length; i++) {
                    tile[i] = new Rectangle(x(i) * width, y(i) * height, width, height);
                }

                for (char color : WB.toCharArray()) {
                    PIECE[WB.indexOf(color)][0] = Toolkit.getDefaultToolkit().createImage(color + ".png").getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    PIECE[WB.indexOf(color)][1] = Toolkit.getDefaultToolkit().createImage(color + "k.png").getScaledInstance(width, height, Image.SCALE_SMOOTH);
                }
            }
        });
    }
    
    static int x(int index) {
        return index % (GRID / 2) * 2 + 1 - index / (GRID / 2) % 2;
    }
    
    static int y(int index) {
        return index / (GRID / 2);
    }
    
    static void paintTile(Graphics g, Rectangle tile) {
        g.fillRect(tile.x, tile.y, tile.width, tile.height);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Rectangle tile : tile) {
            paintTile(g, tile);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Rectangle tile : tile) {
            tile.setLocation(getWidth() - tile.x - tile.width, getHeight() - tile.y - tile.height);
        }
        
        repaint();
    }
    
    public static void main(String[] args) throws IOException {
        int boardSize = 400;
        
        JFrame frame = new JFrame("International Draughts");
        
        JMenuBar menuBar = new JMenuBar();
        
        JMenu gameMenu = menuBar.add(new JMenu("Game"));
        JMenu aiMenu = menuBar.add(new JMenu("AI"));
        
        JButton rotate = new JButton("\ud83d\udd04");
        
        JPanel center = new JPanel();
        JPanel south = new JPanel(new GridLayout(1, 3));
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        for (int color : new int[] {WHITE, BLACK}) {
            gameMenu.add(COLOR[color]).addActionListener(e -> {
                BOARD.remove(game);
                
                game = new Game(color);
                
                BOARD.add(game);
                BOARD.validate();
            });
        }
                
        aiMenu.add(LEVEL);
                
        LEVEL.setMajorTickSpacing(1);
        LEVEL.setPaintLabels(true);
        
        BOARD.setPreferredSize(new Dimension(boardSize, boardSize));
        BOARD.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                ARROW.setVisible(false);
                ARROW.setEnabled(false);
                ARROW.addActionListener((Game) e.getChild());
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                ARROW.removeActionListener((Game) e.getChild());
    
                WINNER.setText("");
            }
        });
        BOARD.add(game, BorderLayout.CENTER);
        
        ARROW.setContentAreaFilled(false);
        ARROW.setBorder(null);
        ARROW.setFont(ARROW.getFont().deriveFont(22f));
        ARROW.setFocusable(false);
        
        WINNER.setHorizontalAlignment(JLabel.CENTER);
        
        MOVEABLE.setFocusable(false);
        MOVEABLE.addActionListener(e -> game.repaint());
        
        rotate.setContentAreaFilled(false);
        rotate.setBorder(null);
        rotate.setFont(rotate.getFont().deriveFont(Font.PLAIN, 14));
        rotate.setFocusable(false);
        rotate.addActionListener(BOARD);
        
        left.add(ARROW);
        
        right.add(MOVEABLE);
        right.add(rotate);
        
        center.add(BOARD);

        south.add(left);
        south.add(WINNER);
        south.add(right);

        frame.setIconImage(Toolkit.getDefaultToolkit().createImage("bk.png").getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setJMenuBar(menuBar);
        frame.add(center, BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }
    
}
