import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable{

    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    final int FPS = 60;
    Thread gamThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP, checkingP;
    public static Piece castlingP;

    // COLOR OF PIECES
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameover;
    boolean stalemate;

    // TIME
    private long whiteTimeLeft;
    private long blackTimeLeft;
    private long timeLimit;

    private boolean timeSelected = false;
    private boolean timeOut = false;

    private String winnerOnTime = "";

    private JButton bulletButton, blitzButton, rapidButton;


    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        setLayout(null);  

        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        bulletButton = new JButton("Bullet (1 min)");
        blitzButton  = new JButton("Blitz (5 min)");
        rapidButton  = new JButton("Rapid (10 min)");

        bulletButton.setBounds(875, 300, 150, 40);
        blitzButton.setBounds(875, 350, 150, 40);
        rapidButton.setBounds(875, 400, 150, 40);

        bulletButton.addActionListener(e -> setTimeControl(1));
        blitzButton.addActionListener(e -> setTimeControl(5));
        rapidButton.addActionListener(e -> setTimeControl(10));

        add(bulletButton);
        add(blitzButton);
        add(rapidButton);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gamThread = new Thread(this);
        gamThread.start();
    }

    public void setPieces() {

        // WHITE TEAM
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        // BLACK TEAM
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {

        target.clear();
        for(int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        long lastTimeMs = System.currentTimeMillis();

        while (gamThread != null) {

            long currentTimeMs = System.currentTimeMillis();
            long elapsed = currentTimeMs - lastTimeMs;  
            lastTimeMs = currentTimeMs;

            if (timeSelected && !gameover && !stalemate && !timeOut) {
                if (currentColor == WHITE) {
                    whiteTimeLeft -= elapsed;
                    if (whiteTimeLeft <= 0) {
                        whiteTimeLeft = 0;
                        timeOut = true;
                        winnerOnTime = "Black";
                    }
                } else {
                    blackTimeLeft -= elapsed;
                    if (blackTimeLeft <= 0) {
                        blackTimeLeft = 0;
                        timeOut = true;
                        winnerOnTime = "White";
                    }
                }
            }

            update();
            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {

        if (promotion) {
            promoting();
        } else if (!gameover && !stalemate && !timeOut) {

            // MOUSE BUTTON PRESSED
            
            if (mouse.pressed) {
                if (activeP == null) {

                    for (Piece piece : simPieces) {

                        if (piece.color == currentColor &&
                            piece.col == mouse.x/Board.SQUARE_SIZE &&
                            piece.row == mouse.y/Board.SQUARE_SIZE) {

                            activeP = piece;
                        }
                    }
                } else {
                    simulate();
                }
            }

            // MOUSE BUTTON RELEASED

            if (mouse.pressed == false) {

                if (activeP != null) {
                    
                    
                    if (validSquare) {

                        // MOVE CONFIRMED

                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();

                        if (castlingP != null) {
                            castlingP.updatePosition();
                        }

                        if (isKingInCheck() && isCheckmate()) {
                            gameover = true;
                        } else if (isStalemate() && isKingInCheck() == false) {
                            stalemate = true;
                        } else {
                            if (canPromote()) {
                                promotion = true;
                            } else {
                                changePlayer();
                            }   
                        }
                    } else {

                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                        activeP = null;
                    }
                }
            }
        }
    }

    private void simulate() {

        canMove = false;
        validSquare = false;

        copyPieces(pieces, simPieces);

        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (activeP.canMove(activeP.col, activeP.row)) {

            canMove = true;

            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }

            checkCastling();

            if (isIllegal(activeP) == false && opponentCanCaptureKing() == false) {
                validSquare = true;
            }
        }
    }

    private boolean isIllegal(Piece king) {

        if (king.type == Type.KING) {
            for (Piece piece : simPieces) {
                if (piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean opponentCanCaptureKing() {

        Piece king = getKing(false);

        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }

        return false;
    }

    private boolean isKingInCheck() {

        Piece king = getKing(true);

        if (activeP.canMove(king.col, king.row)) {
            checkingP = activeP;
            return true;
        } else {
            checkingP = null;
        }

        return false;
    }

    private Piece getKing(boolean opponent) {

        Piece king = null;

        for (Piece piece : simPieces) {
            if (opponent) {
                if (piece.type == Type.KING && piece.color != currentColor) {
                    king = piece;
                }
            } else {
                if (piece.type == Type.KING && piece.color == currentColor) {
                    king = piece;
                }
            }
        }

        return king;
    }

    private boolean isCheckmate() {

        Piece king = getKing(true);

        if (kingCanMove(king)) {
            return false;
        } else {

            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);

            if (colDiff == 0) {

                // CHECK IS APPLAIED VERTICALLY

                if (checkingP.row < king.row) {

                    // CHECK PIECE IS ABOVE THE KING 

                    for (int row = checkingP.row; row < king.row; row++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }

                if (checkingP.row > king.row) {

                    // CHECK PIECE IS BELOW THE KING 

                    for (int row = checkingP.row; row > king.row; row--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }

            } else if (rowDiff == 0) {

                // CHECK IS APPLAIED HORIZONATLLY

                if (checkingP.col < king.col) {

                    // CHECK PIECE IS ON THE LEFT OF THE KING 

                    for (int col = checkingP.col; col < king.col; col++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }

                if (checkingP.col > king.col) {

                    // CHECK PIECE IS ON THE RIGHT OF THE KING 

                    for (int col = checkingP.col; col > king.col; col--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }

            } else if (colDiff == rowDiff) {

                // CHECK IS APPLAIED DIAGONALLY

                if (checkingP.row < king.row) {

                    // CHECK PIECE IS ABOVE THE KING 

                    if (checkingP.col < king.col) {

                        // CHECK PIECE IS ON THE UPPER LEFT OF THE KING 

                        for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }

                    if (checkingP.col > king.col) {

                        // CHECK PIECE IS ON THE UPPER RIGHT OF THE KING

                        for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }

                if (checkingP.row > king.row) {

                    // CHECK PIECE IS BELOW THE KING 

                    if (checkingP.col < king.col) {

                        // CHECK PIECE IS ON THE LOWER LEFT OF THE KING 

                        for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }

                    if (checkingP.col > king.col) {

                        // CHECK PIECE IS ON THE LOWER RIGHT OF THE KING

                        for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }

            }
        }

        return true;
    }

    private boolean kingCanMove(Piece king) {

        if (isValidMove(king, -1, -1)) {return true;}
        if (isValidMove(king, 0, -1)) {return true;}
        if (isValidMove(king, 1, -1)) {return true;}
        if (isValidMove(king, -1, 0)) {return true;}
        if (isValidMove(king, 1, 0)) {return true;}
        if (isValidMove(king, -1, 1)) {return true;}
        if (isValidMove(king, 0, 1)) {return true;}
        if (isValidMove(king, 1, 1)) {return true;}

        return false;
    }

    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {

        boolean isValidMove = false;

        king.col += colPlus;
        king.row += rowPlus;

        if (king.canMove(king.col, king.row)) {
            
            if (king.hittingP != null) {
                simPieces.remove(king.hittingP.getIndex());
            }

            if (isIllegal(king) == false) {
                isValidMove = true;
            }
        }

        king.resetPosition();
        copyPieces(pieces, simPieces);

        return isValidMove;
    }

    private boolean isStalemate() {

        int count = 0;

        for (Piece piece : simPieces) {
            if (piece.color != currentColor) {
                count++;
            }
        }

        if (count == 1) {
            if (kingCanMove(getKing(true)) == false) {
                return true;
            }
        }

        return false;
    }

    private void checkCastling() {

        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if ( castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        if (currentColor == WHITE) {
            currentColor = BLACK;
        } else {
            currentColor = WHITE;
        }

        activeP = null;
    }

    private boolean canPromote() {

        if (activeP.type == Type.PAWN) {
            if (currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }

        return false;
    }

    private void promoting() {

        if (mouse.pressed) {
            for (Piece piece : promoPieces) {
                if (piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
                    switch (piece.type) {
                        case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
                        case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
                        case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
                        case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
                        default: break;
                    }

                    simPieces.remove(activeP.getIndex());
                    copyPieces(simPieces, pieces);
                    activeP = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    private void setTimeControl(int minutes) {
        timeLimit = minutes * 60_000L;  
        whiteTimeLeft = timeLimit;
        blackTimeLeft = timeLimit;
        timeSelected = true;
        timeOut = false;
        winnerOnTime = "";
    
        bulletButton.setVisible(false);
        blitzButton.setVisible(false);
        rapidButton.setVisible(false);
    
        if (gamThread == null) {
            launchGame();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D)g;

        // BOARD
        board.draw(g2);

        // PIECES
        for(Piece p : simPieces) {
            p.draw(g2);
        }

        // TIME
        if (!timeSelected) {
            return;
        }

        if (timeSelected) {
            g2.setFont(new Font(null, Font.PLAIN, 24));
            g2.setColor(Color.white);
    
            String blackClock = formatTime(blackTimeLeft);
            String whiteClock = formatTime(whiteTimeLeft);
    
            g2.drawString(blackClock, 820, 35);
            g2.drawString(whiteClock, 820, 780);
        } 
    
        if (timeOut) {
            g2.setColor(Color.red);
            g2.setFont(new Font(null, Font.BOLD, 40));
            g2.drawString(winnerOnTime + " wins on time!", 200, 350);
        }

        // HOVER ANIMATION
        if (activeP != null) {
            if (canMove) {
                if (isIllegal(activeP) || opponentCanCaptureKing()) {
                    g2.setColor(Color.red);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                } else {
                    g2.setColor(Color.white);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }
            
            activeP.draw(g2);
        }

        // STATUS MESSAGES
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(null, Font.PLAIN, 40));
        g2.setColor(Color.white);

        if (promotion) {
            g2.drawString("Promote to:", 840, 150);
            for (Piece piece : promoPieces) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
                                Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        } else {
            if (currentColor == WHITE) {
                g2.drawString("White's turn", 840, 550);

                if (checkingP != null && checkingP.color == BLACK) {
                    g2.setColor(Color.red);
                    g2.drawString("The king", 840, 650);
                    g2.drawString("is in check!", 840, 700);
                }
            } else {
                g2.drawString("Black's turn", 840, 250);

                if (checkingP != null && checkingP.color == WHITE) {
                    g2.setColor(Color.red);
                    g2.drawString("The king", 840, 100);
                    g2.drawString("is in check!", 840, 150);
                }
            }
        }

        if (gameover) {
            String s = "";

            if (currentColor == WHITE) {
                s = "White wins";
            } else {
                s = "Black wins";
            }

            g2.setFont(new Font(null, Font.PLAIN, 90));
            g2.setColor(Color.green);
            g2.drawString(s, 200, 420);
        }

        if (stalemate) {
            g2.setFont(new Font(null, Font.PLAIN, 90));
            g2.setColor(Color.lightGray);
            g2.drawString("Stalemate", 200, 420);
        }
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
