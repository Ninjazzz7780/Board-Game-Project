import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.File;

public class SnakesLaddersGame extends JFrame {
    private String gameState = "menu";
    private int numPlayers = 2;
    private ArrayList<Player> players;
    private PlayerQueue playerQueue;
    private Player currentPlayer;
    private Integer diceResult = null;
    private Random random;

    private Map<Integer, Integer> links;
    private Map<Integer, Integer> nodePoints;
    private PriorityQueue<NodePoint> pointPQ;
    private Sound sound;

    // --- VARIABEL HISTORY / LEADERBOARD ---
    private GameHistoryManager historyManager;
    private JPanel historyListPanel;
    // --------------------------------------

    // --- VARIABEL ANIMASI SLIDING ---
    private Player slidingPlayer = null;
    private Point slidingPlayerPos = null;
    // --------------------------------

    private final int BOARD_SIZE = 64;
    private final int MAX_LINKS = 5;

    private String[] playerColors = {"#FF6B6B", "#4ECDC4", "#FFD93D", "#95E1D3", "#F38181", "#AA96DA"};

    private JPanel mainPanel;
    private CardLayout cardLayout;

    // --- UI VARIABLES ---
    private JPanel gamePanel;
    private JLayeredPane boardContainer;
    private JPanel gridPanel;
    private BoardDrawingPanel drawingLayer; // Error solved: Class didefinisikan di bawah
    private JLabel currentPlayerLabel;
    private JLabel currentPlayerNameLabel;
    private JLabel currentPlayerPosLabel;
    private JLabel currentPlayerPointLabel;
    private JButton rollDiceButton;
    private DicePanel dicePanel;
    private JLabel diceDirectionLabel;
    private JPanel playersListPanel;
    private JPanel winnerDisplayPanel;
    // ----------------------------

    private static class NodePoint {
        private final int node;
        private final int point;

        public NodePoint(int node, int point) {
            this.node = node;
            this.point = point;
        }
        public int getNode() { return node; }
        public int getPoint() { return point; }
    }

    public SnakesLaddersGame() {
        setTitle("Board Game");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        players = new ArrayList<>();
        playerQueue = new PlayerQueue();
        random = new Random();
        nodePoints = new HashMap<>();
        pointPQ = new PriorityQueue<>(Comparator.comparingInt(NodePoint::getPoint).reversed());

        this.sound = new Sound();
        this.historyManager = new GameHistoryManager();

        initializeLinks();
        initializeNodePoints();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initMenuScreen();
        initSetupScreen();
        initGameScreen();
        initWinnerScreen();
        initHistoryScreen();

        add(mainPanel);
        cardLayout.show(mainPanel, "menu");

        sound.playBacksound("assets/background_music.wav");
    }

    // --- INNER CLASS: BoardDrawingPanel (SOLUSI ERROR 1) ---
    private class BoardDrawingPanel extends JPanel {
        public BoardDrawingPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int boardWidth = getWidth();
            int cellSize = boardWidth / 8;

            // Gambar Tangga
            for (Map.Entry<Integer, Integer> entry : links.entrySet()) {
                int startPos = entry.getKey();
                int endPos = entry.getValue();

                Point startCoord = getCenterCoordinates(startPos, cellSize);
                Point endCoord = getCenterCoordinates(endPos, cellSize);

                Color linkColor = new Color(34, 139, 34, 200); // Hijau
                g2d.setColor(linkColor);

                int width = Math.max(6, cellSize / 8);
                g2d.setStroke(new BasicStroke(3));

                double dx = endCoord.x - startCoord.x;
                double dy = endCoord.y - startCoord.y;
                double length = Math.sqrt(dx * dx + dy * dy);

                if (length == 0) continue;

                double ux = dy / length;
                double uy = -dx / length;

                int x1_a = startCoord.x + (int)(width * ux);
                int y1_a = startCoord.y + (int)(width * uy);
                int x2_a = endCoord.x + (int)(width * ux);
                int y2_a = endCoord.y + (int)(width * uy);

                int x1_b = startCoord.x - (int)(width * ux);
                int y1_b = startCoord.y - (int)(width * uy);
                int x2_b = endCoord.x - (int)(width * ux);
                int y2_b = endCoord.y - (int)(width * uy);

                g2d.drawLine(x1_a, y1_a, x2_a, y2_a);
                g2d.drawLine(x1_b, y1_b, x2_b, y2_b);

                g2d.setStroke(new BasicStroke(2));
                int numRungs = (int)(length / cellSize * 3);

                for (int i = 1; i < numRungs; i++) {
                    double ratio = (double)i / numRungs;
                    int x_c = startCoord.x + (int)(dx * ratio);
                    int y_c = startCoord.y + (int)(dy * ratio);
                    int xr1 = x_c + (int)(width * ux);
                    int yr1 = y_c + (int)(width * uy);
                    int xr2 = x_c - (int)(width * ux);
                    int yr2 = y_c - (int)(width * uy);
                    g2d.drawLine(xr1, yr1, xr2, yr2);
                }

                int dotSize = Math.max(10, cellSize/5);
                g2d.fillOval(startCoord.x - dotSize/2, startCoord.y - dotSize/2, dotSize, dotSize);
                g2d.fillOval(endCoord.x - dotSize/2, endCoord.y - dotSize/2, dotSize, dotSize);
            }

            // Animasi Sliding (Pion melayang)
            if (slidingPlayer != null && slidingPlayerPos != null) {
                int pionSize = Math.max(25, cellSize / 3);
                g2d.setColor(Color.decode(slidingPlayer.getColor()));

                // Shadow
                g2d.setColor(new Color(0,0,0, 50));
                g2d.fillOval(slidingPlayerPos.x - pionSize/2 + 3, slidingPlayerPos.y - pionSize/2 + 3, pionSize, pionSize);

                // Pion
                g2d.setColor(Color.decode(slidingPlayer.getColor()));
                g2d.fillOval(slidingPlayerPos.x - pionSize/2, slidingPlayerPos.y - pionSize/2, pionSize, pionSize);

                // Border
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(slidingPlayerPos.x - pionSize/2, slidingPlayerPos.y - pionSize/2, pionSize, pionSize);

                // Initial Name
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                String initial = slidingPlayer.getName().substring(slidingPlayer.getName().length()-1);
                FontMetrics fm = g2d.getFontMetrics();
                int tx = slidingPlayerPos.x - fm.stringWidth(initial)/2;
                int ty = slidingPlayerPos.y + fm.getAscent()/2 - 2;
                g2d.drawString(initial, tx, ty);
            }
        }
    }
    // --------------------------------------------------------

    // --- METHOD UPDATE BOARD (SOLUSI ERROR 2) ---
    private void updateBoard() {
        gridPanel.removeAll();
        int boardW = gridPanel.getWidth();
        int cellW = Math.max(20, boardW / 8);

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                int cellNumber = (row % 2 == 0) ? row * 8 + col : row * 8 + (7 - col);
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground((cellNumber + 1) % 2 == 0 ? new Color(236, 240, 241) : Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

                JLabel cellLabel = new JLabel(String.valueOf(cellNumber + 1));
                cellLabel.setFont(new Font("Arial", Font.PLAIN, Math.max(10, cellW/5)));
                cellLabel.setForeground(Color.GRAY);
                cell.add(cellLabel, BorderLayout.NORTH);

                if (nodePoints.containsKey(cellNumber)) {
                    JLabel pointLabel = new JLabel("+" + nodePoints.get(cellNumber));
                    pointLabel.setFont(new Font("Arial", Font.BOLD, Math.max(9, cellW/7)));
                    pointLabel.setForeground(new Color(39, 174, 96));
                    pointLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                    cell.add(pointLabel, BorderLayout.SOUTH);
                }

                JPanel pionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
                pionsPanel.setOpaque(false);

                for (Player player : players) {
                    if (player == slidingPlayer) continue; // Hide original pion during sliding animation

                    if (player.getPosition() == cellNumber) {
                        JLabel pion = new JLabel(player.getName().substring(player.getName().length() - 1));
                        int pionSize = Math.max(15, cellW / 3);
                        pion.setPreferredSize(new Dimension(pionSize, pionSize));
                        pion.setOpaque(true);
                        pion.setBackground(Color.decode(player.getColor()));
                        pion.setForeground(Color.WHITE);
                        pion.setFont(new Font("Arial", Font.BOLD, Math.max(10, pionSize/2)));
                        pion.setHorizontalAlignment(SwingConstants.CENTER);
                        pion.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                        pionsPanel.add(pion);
                    }
                }
                cell.add(pionsPanel, BorderLayout.CENTER);
                gridPanel.add(cell);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
        drawingLayer.repaint();
    }
    // --------------------------------------------

    // --- LOGIKA GAME LAINNYA ---

    private void initializeNodePoints() {
        nodePoints.clear();
        pointPQ.clear();

        for (int i = 1; i < BOARD_SIZE - 1; i++) {
            int point = 1 + random.nextInt(25);
            nodePoints.put(i, point);
            pointPQ.offer(new NodePoint(i, point));
        }
    }

    private void claimNodePoint(Player player) {
        int currentPos = player.getPosition();

        if (nodePoints.containsKey(currentPos)) {
            int point = nodePoints.get(currentPos);
            player.addPoint(point);

            nodePoints.remove(currentPos);
            pointPQ.removeIf(np -> np.getNode() == currentPos);
        }
    }

    private void applyRgbBackground(JPanel panel, int r, int g, int b) {
        panel.setBackground(new Color(r, g, b));
    }

    private void initializeLinks() {
        links = new HashMap<>();
        List<Integer> availableCells = new ArrayList<>();
        for (int i = 1; i < BOARD_SIZE - 9; i++) {
            availableCells.add(i);
        }

        Collections.shuffle(availableCells, random);

        int count = 0;
        for (int start : availableCells) {
            if (count >= MAX_LINKS) break;

            int minEnd = start + 1;
            int maxEnd = BOARD_SIZE - 2;
            if (minEnd >= maxEnd) continue;

            int end = minEnd + random.nextInt(maxEnd - minEnd);

            if (!links.containsKey(start) && !links.containsValue(end) && !links.containsKey(end)) {
                links.put(start, end);
                count++;
            }
        }
    }

    private Point getCenterCoordinates(int position, int cellSize) {
        int boardPos = position + 1;
        int rowFromBottom = (boardPos - 1) / 8;
        int colIndex;
        if (rowFromBottom % 2 == 0) colIndex = (boardPos - 1) % 8;
        else colIndex = 7 - ((boardPos - 1) % 8);

        int screenRowIndex = 7 - rowFromBottom;
        int x = colIndex * cellSize + cellSize / 2;
        int y = screenRowIndex * cellSize + cellSize / 2;
        return new Point(x, y);
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    private void animateLadderSlide(Player player, int startIdx, int endIdx) {
        slidingPlayer = player;
        int cellSize = gridPanel.getWidth() / 8;
        Point start = getCenterCoordinates(startIdx, cellSize);
        Point end = getCenterCoordinates(endIdx, cellSize);

        final long duration = 1500;
        final long startTime = System.currentTimeMillis();

        Timer slideTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                float fraction = (float)(now - startTime) / duration;

                if (fraction >= 1.0f) {
                    ((Timer)e.getSource()).stop();
                    slidingPlayer = null;
                    slidingPlayerPos = null;

                    player.setPosition(endIdx);
                    player.addStep(new Step("link_up", endIdx - startIdx, endIdx));

                    sound.playSoundEffect("assets/move.wav");
                    JOptionPane.showMessageDialog(gamePanel, "Naik Tangga! Bonus!", "Link", JOptionPane.INFORMATION_MESSAGE);

                    updateGameScreen();

                    claimNodePoint(player);
                    finalizeTurn(player);
                } else {
                    int curX = (int)(start.x + (end.x - start.x) * fraction);
                    int curY = (int)(start.y + (end.y - start.y) * fraction);
                    slidingPlayerPos = new Point(curX, curY);

                    drawingLayer.repaint();
                    gridPanel.repaint();
                }
            }
        });
        slideTimer.start();
    }

    private void startDijkstraAutoMove(Player player) {
        Graph graph = new Graph(BOARD_SIZE);
        graph.buildGraphWithLinks(BOARD_SIZE, links);

        List<Integer> path = Dijkstra.findShortestPath(graph, player.getPosition(), BOARD_SIZE - 1);

        if (path.isEmpty() || path.size() <= 1) {
            JOptionPane.showMessageDialog(this, "Tidak ada jalur otomatis ditemukan, gulir dadu manual!");
            return;
        }

        int nextOptimalNode = path.get(1);
        int diceNeeded = -1;

        for (int d = 1; d <= 6; d++) {
            int possibleMove = player.getPosition() + d;
            if (links.containsKey(possibleMove)) {
                possibleMove = links.get(possibleMove);
            }
            if (possibleMove == nextOptimalNode) {
                diceNeeded = d;
                break;
            }
        }

        if (diceNeeded != -1) {
            diceResult = diceNeeded;
            Color dotColor = new Color(34, 139, 34);
            dicePanel.setValue(diceNeeded, dotColor);
            diceDirectionLabel.setText("Auto: " + diceNeeded);
            diceDirectionLabel.setForeground(dotColor);
            rollDiceButton.setEnabled(false);
            animateMove(player, player.getPosition(), player.getPosition() + diceNeeded, diceNeeded, true);
        } else {
            rollDice();
        }
    }

    private void animateMove(Player player, int startPos, int endPos, int totalSteps, boolean isForward) {
        final int steps = isForward ? 1 : -1;
        final int initialPos = startPos;
        final int stepsLimit = totalSteps;

        Timer animationTimer = new Timer(300, new ActionListener() {
            private int currentStep = 0;
            private int currentPos = initialPos;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentStep++;

                if (currentStep <= stepsLimit) {
                    currentPos += steps;
                    currentPos = Math.min(Math.max(currentPos, 0), BOARD_SIZE - 1);

                    player.setPosition(currentPos);

                    sound.playSoundEffect("assets/move.wav");

                    updateGameScreen();
                }

                if (currentStep == stepsLimit || currentPos == BOARD_SIZE - 1) {
                    ((Timer)e.getSource()).stop();
                    String direction = isForward ? "forward" : "backward";
                    player.addStep(new Step(direction, totalSteps, player.getPosition()));
                    checkPostMoveLogic(player);
                }
            }
        });
        animationTimer.setRepeats(true);
        animationTimer.start();
    }

    private void checkPostMoveLogic(Player player) {
        if (player.getPosition() >= BOARD_SIZE - 1) {
            gameState = "finished";
            showWinnerRank();
            return;
        }

        if (links.containsKey(player.getPosition())) {
            int target = links.get(player.getPosition());
            animateLadderSlide(player, player.getPosition(), target);
            return;
        }

        claimNodePoint(player);
        finalizeTurn(player);
    }

    private void finalizeTurn(Player player) {
        if (player.getPosition() >= BOARD_SIZE - 1) {
            gameState = "finished";
            showWinnerRank();
            return;
        }

        boolean isDoubleTurn = (player.getPosition() + 1) % 5 == 0;

        Timer turnTimer = new Timer(500, e -> {
            if (!isDoubleTurn) {
                nextPlayer();
            } else {
                JOptionPane.showMessageDialog(this, player.getName() + " mendapat Double Turn! Bermain lagi.", "Double Turn!", JOptionPane.INFORMATION_MESSAGE);
            }
            diceResult = null;
            diceDirectionLabel.setText("");
            dicePanel.setValue(1, Color.BLACK);
            rollDiceButton.setEnabled(true);
            updateCurrentPlayer();
            updatePlayersList();
        });
        turnTimer.setRepeats(false);
        turnTimer.start();
    }

    private void rollDice() {
        // Logika 70% Maju, 30% Mundur
        boolean isPrimeNode = isPrime(currentPlayer.getPosition() + 1);
        int diceValue = random.nextInt(6) + 1;
        diceResult = diceValue;

        double r = random.nextDouble();
        boolean isForward = isPrimeNode || (r <= 0.7);

        Color dotColor;
        String directionText;

        if (isForward) {
            dotColor = new Color(39, 174, 96); // Hijau
            directionText = "Maju: " + diceValue;
        } else {
            dotColor = new Color(192, 57, 43); // Merah
            directionText = "Mundur: " + diceValue;
        }

        dicePanel.setValue(diceValue, dotColor);
        diceDirectionLabel.setText(directionText);
        diceDirectionLabel.setForeground(dotColor);

        rollDiceButton.setEnabled(false);

        int targetPos;
        if (isForward) {
            targetPos = currentPlayer.getPosition() + diceValue;
        } else {
            targetPos = currentPlayer.getPosition() - diceValue;
            if (targetPos < 0) targetPos = 0;
        }

        animateMove(currentPlayer, currentPlayer.getPosition(), targetPos, diceValue, isForward);
    }

    private void nextPlayer() {
        playerQueue.poll();
        playerQueue.enqueue(currentPlayer);
        currentPlayer = playerQueue.peek();
    }

    // --- UI SETUP & SCREENS ---

    private void setupPlayers() {
        players.clear();
        playerQueue = new PlayerQueue();
        initializeLinks();
        initializeNodePoints();

        for (int i = 0; i < numPlayers; i++) {
            Player player = new Player("Player " + (i + 1), playerColors[i]);
            players.add(player);
            playerQueue.enqueue(player);
        }

        currentPlayer = playerQueue.peek();
        diceResult = null;
    }

    private void updateGameScreen() {
        updateBoard();
        updateCurrentPlayer();
        updatePlayersList();
    }

    private void updateCurrentPlayer() {
        if (currentPlayer != null) {
            currentPlayerLabel.setText(currentPlayer.getName().substring(currentPlayer.getName().length() - 1));
            currentPlayerLabel.setBackground(Color.decode(currentPlayer.getColor()));
            currentPlayerNameLabel.setText(currentPlayer.getName());
            currentPlayerPosLabel.setText("Position: " + (currentPlayer.getPosition() + 1));
            currentPlayerPointLabel.setText("Points: " + currentPlayer.getPoint());
        }
    }

    private void updatePlayersList() {
        playersListPanel.removeAll();
        List<Player> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort(Comparator.comparingInt(Player::getPoint).reversed());

        for (Player player : sortedPlayers) {
            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            if (player == currentPlayer) {
                playerPanel.setBackground(new Color(214, 234, 248));
                playerPanel.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2));
            } else {
                playerPanel.setBackground(new Color(245, 245, 245));
            }

            JLabel pion = new JLabel(player.getName().substring(player.getName().length() - 1));
            pion.setPreferredSize(new Dimension(30, 30));
            pion.setOpaque(true);
            pion.setBackground(Color.decode(player.getColor()));
            pion.setForeground(Color.WHITE);
            pion.setFont(new Font("Arial", Font.BOLD, 14));
            pion.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel info = new JLabel(player.getName() + " - Pos: " + (player.getPosition() + 1) + " - Pts: " + player.getPoint());
            info.setFont(new Font("Arial", Font.PLAIN, 12));
            playerPanel.add(pion);
            playerPanel.add(info);
            playersListPanel.add(playerPanel);
        }
        playersListPanel.revalidate();
        playersListPanel.repaint();
    }

    private void initMenuScreen() {
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridBagLayout());
        applyRgbBackground(menuPanel, 52, 152, 219);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(20, 20, 20, 20);

        JLabel titleLabel = new JLabel("BOARD GAME");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        menuPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        JButton playButton = new JButton("PLAY");
        playButton.setFont(new Font("Arial", Font.BOLD, 24));
        playButton.setPreferredSize(new Dimension(200, 60));
        playButton.setBackground(new Color(46, 204, 113));
        playButton.setForeground(Color.WHITE);
        playButton.setFocusPainted(false);
        playButton.addActionListener(e -> {
            gameState = "setup";
            cardLayout.show(mainPanel, "setup");
        });
        menuPanel.add(playButton, gbc);
        mainPanel.add(menuPanel, "menu");
    }

    private void initSetupScreen() {
        JPanel setupPanel = new JPanel();
        setupPanel.setLayout(new GridBagLayout());
        applyRgbBackground(setupPanel, 52, 152, 219);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel setupLabel = new JLabel("Setup Game");
        setupLabel.setFont(new Font("Arial", Font.BOLD, 36));
        setupLabel.setForeground(Color.WHITE);
        setupPanel.add(setupLabel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel playerLabel = new JLabel("Jumlah Player:");
        playerLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        playerLabel.setForeground(Color.WHITE);
        setupPanel.add(playerLabel, gbc);

        gbc.gridx = 1;
        SpinnerModel spinnerModel = new SpinnerNumberModel(2, 2, 6, 1);
        JSpinner playerSpinner = new JSpinner(spinnerModel);
        playerSpinner.setFont(new Font("Arial", Font.PLAIN, 20));
        ((JSpinner.DefaultEditor) playerSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
        setupPanel.add(playerSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 20));
        startButton.setPreferredSize(new Dimension(200, 50));
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> {
            numPlayers = (Integer) playerSpinner.getValue();
            setupPlayers();
            gameState = "playing";
            cardLayout.show(mainPanel, "game");
            updateGameScreen();
        });
        setupPanel.add(startButton, gbc);
        mainPanel.add(setupPanel, "setup");
    }

    private void initGameScreen() {
        gamePanel = new JPanel(new BorderLayout(20, 10));
        applyRgbBackground(gamePanel, 52, 152, 219);
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Board Game", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        gamePanel.add(titleLabel, BorderLayout.NORTH);

        boardContainer = new JLayeredPane();
        gridPanel = new JPanel(new GridLayout(8, 8, 2, 2));
        gridPanel.setOpaque(true);
        gridPanel.setBackground(new Color(236, 240, 241));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));

        drawingLayer = new BoardDrawingPanel(); // Sudah didefinisikan sebagai inner class
        drawingLayer.setOpaque(false);

        boardContainer.add(gridPanel, JLayeredPane.DEFAULT_LAYER);
        boardContainer.add(drawingLayer, JLayeredPane.PALETTE_LAYER);

        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.setOpaque(false);
        boardWrapper.add(boardContainer);

        boardWrapper.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = boardWrapper.getWidth();
                int h = boardWrapper.getHeight();
                int size = Math.min(w, h) - 20;
                if (size < 100) size = 100;
                boardContainer.setPreferredSize(new Dimension(size, size));
                gridPanel.setBounds(0, 0, size, size);
                drawingLayer.setBounds(0, 0, size, size);
                boardContainer.revalidate();
                boardWrapper.revalidate();
            }
        });
        gamePanel.add(boardWrapper, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);
        controlPanel.setPreferredSize(new Dimension(350, 800));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel currentRollPlayersPanel = new JPanel();
        currentRollPlayersPanel.setLayout(new BoxLayout(currentRollPlayersPanel, BoxLayout.Y_AXIS));
        currentRollPlayersPanel.setOpaque(false);

        // Current Player
        JPanel currentPlayerPanel = new JPanel();
        currentPlayerPanel.setLayout(new BoxLayout(currentPlayerPanel, BoxLayout.Y_AXIS));
        currentPlayerPanel.setBackground(Color.WHITE);
        currentPlayerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        currentPlayerPanel.setMaximumSize(new Dimension(350, 250));
        currentPlayerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cpTitle = new JLabel("Current Player");
        cpTitle.setFont(new Font("Arial", Font.BOLD, 22));
        cpTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentPlayerPanel.add(cpTitle);
        currentPlayerPanel.add(Box.createVerticalStrut(15));

        currentPlayerLabel = new JLabel();
        currentPlayerLabel.setPreferredSize(new Dimension(80, 80));
        currentPlayerLabel.setOpaque(true);
        currentPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        currentPlayerLabel.setForeground(Color.WHITE);
        currentPlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentPlayerPanel.add(currentPlayerLabel);

        currentPlayerNameLabel = new JLabel();
        currentPlayerNameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        currentPlayerNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentPlayerPanel.add(Box.createVerticalStrut(10));
        currentPlayerPanel.add(currentPlayerNameLabel);

        currentPlayerPosLabel = new JLabel();
        currentPlayerPosLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        currentPlayerPosLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentPlayerPanel.add(currentPlayerPosLabel);

        currentPlayerPointLabel = new JLabel();
        currentPlayerPointLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentPlayerPointLabel.setForeground(new Color(39, 174, 96));
        currentPlayerPointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentPlayerPanel.add(currentPlayerPointLabel);

        currentRollPlayersPanel.add(currentPlayerPanel);
        currentRollPlayersPanel.add(Box.createVerticalStrut(20));

        // Roll Dice
        JPanel rollDicePanel = new JPanel();
        rollDicePanel.setLayout(new BoxLayout(rollDicePanel, BoxLayout.Y_AXIS));
        rollDicePanel.setBackground(Color.WHITE);
        rollDicePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        rollDicePanel.setMaximumSize(new Dimension(350, 300));
        rollDicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rollDiceButton = new JButton("Roll Dice");
        rollDiceButton.setFont(new Font("Arial", Font.BOLD, 20));
        rollDiceButton.setBackground(new Color(52, 152, 219));
        rollDiceButton.setForeground(Color.WHITE);
        rollDiceButton.setFocusPainted(false);
        rollDiceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollDiceButton.addActionListener(e -> rollDice());
        rollDicePanel.add(rollDiceButton);

        rollDicePanel.add(Box.createVerticalStrut(20));

        dicePanel = new DicePanel();
        dicePanel.setPreferredSize(new Dimension(100, 100));
        dicePanel.setMaximumSize(new Dimension(100, 100));
        dicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollDicePanel.add(dicePanel);

        rollDicePanel.add(Box.createVerticalStrut(15));

        diceDirectionLabel = new JLabel("");
        diceDirectionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        diceDirectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollDicePanel.add(diceDirectionLabel);

        JButton debugDijkstraButton = new JButton("Auto Move (Dijkstra)");
        debugDijkstraButton.setFont(new Font("Arial", Font.PLAIN, 12));
        debugDijkstraButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        debugDijkstraButton.addActionListener(e -> {
            startDijkstraAutoMove(currentPlayer);
        });
        // Uncomment to show cheat button
        // rollDicePanel.add(debugDijkstraButton);

        currentRollPlayersPanel.add(rollDicePanel);
        currentRollPlayersPanel.add(Box.createVerticalStrut(20));

        // Players List
        JPanel playersContainer = new JPanel();
        playersContainer.setLayout(new BoxLayout(playersContainer, BoxLayout.Y_AXIS));
        playersContainer.setBackground(Color.WHITE);
        playersContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        playersContainer.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));
        playersContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel playersTitle = new JLabel("All Players");
        playersTitle.setFont(new Font("Arial", Font.BOLD, 22));
        playersTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        playersContainer.add(playersTitle);
        playersContainer.add(Box.createVerticalStrut(15));

        playersListPanel = new JPanel();
        playersListPanel.setLayout(new BoxLayout(playersListPanel, BoxLayout.Y_AXIS));
        playersListPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(playersListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        playersContainer.add(scrollPane);

        currentRollPlayersPanel.add(playersContainer);
        controlPanel.add(currentRollPlayersPanel);
        gamePanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(gamePanel, "game");
    }

    private void initWinnerScreen() {
        JPanel winnerPanel = new JPanel();
        winnerPanel.setLayout(new GridBagLayout());
        applyRgbBackground(winnerPanel, 230, 126, 34);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(20, 20, 20, 20);

        JLabel winnerTitle = new JLabel("GAME OVER! Ranking Pemenang");
        winnerTitle.setFont(new Font("Arial", Font.BOLD, 48));
        winnerTitle.setForeground(Color.WHITE);
        winnerPanel.add(winnerTitle, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        winnerDisplayPanel = new JPanel();
        winnerDisplayPanel.setLayout(new BoxLayout(winnerDisplayPanel, BoxLayout.Y_AXIS));
        winnerDisplayPanel.setBackground(new Color(255, 255, 255, 200));
        winnerDisplayPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        winnerPanel.add(winnerDisplayPanel, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 18));
        playAgainButton.setPreferredSize(new Dimension(180, 50));
        playAgainButton.setBackground(new Color(46, 204, 113));
        playAgainButton.setForeground(Color.WHITE);
        playAgainButton.setFocusPainted(false);
        playAgainButton.addActionListener(e -> {
            resetGame();
            cardLayout.show(mainPanel, "menu");
        });

        JButton leaderboardButton = new JButton("View Leaderboard");
        leaderboardButton.setFont(new Font("Arial", Font.BOLD, 18));
        leaderboardButton.setPreferredSize(new Dimension(220, 50));
        leaderboardButton.setBackground(new Color(52, 152, 219));
        leaderboardButton.setForeground(Color.WHITE);
        leaderboardButton.setFocusPainted(false);
        leaderboardButton.addActionListener(e -> {
            updateHistoryUI();
            cardLayout.show(mainPanel, "history");
        });

        buttonPanel.add(playAgainButton);
        buttonPanel.add(leaderboardButton);
        winnerPanel.add(buttonPanel, gbc);
        mainPanel.add(winnerPanel, "winner");
    }

    private void initHistoryScreen() {
        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new GridBagLayout());
        applyRgbBackground(historyPanel, 44, 62, 80);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel titleLabel = new JLabel("🏆 HALL OF FAME 🏆");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(new Color(255, 215, 0));
        historyPanel.add(titleLabel, gbc);

        gbc.gridy = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;

        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        historyListPanel.setBackground(new Color(255, 255, 255, 20));
        historyListPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JScrollPane scrollPane = new JScrollPane(historyListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        historyPanel.add(scrollPane, gbc);

        gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weighty = 0;

        JButton backToMenuButton = new JButton("Main Menu");
        backToMenuButton.setFont(new Font("Arial", Font.BOLD, 20));
        backToMenuButton.setPreferredSize(new Dimension(250, 50));
        backToMenuButton.setBackground(new Color(231, 76, 60));
        backToMenuButton.setForeground(Color.WHITE);
        backToMenuButton.setFocusPainted(false);
        backToMenuButton.addActionListener(e -> {
            resetGame();
            cardLayout.show(mainPanel, "menu");
        });
        historyPanel.add(backToMenuButton, gbc);
        mainPanel.add(historyPanel, "history");
    }

    private void updateHistoryUI() {
        historyListPanel.removeAll();
        List<Map.Entry<String, Integer>> history = historyManager.getSortedHistory();

        if (history.isEmpty()) {
            JLabel emptyLabel = new JLabel("Belum ada data kemenangan.");
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 18));
            emptyLabel.setForeground(Color.WHITE);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            historyListPanel.add(emptyLabel);
        } else {
            int rank = 1;
            for (Map.Entry<String, Integer> entry : history) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(new Color(0, 0, 0, 0));
                row.setMaximumSize(new Dimension(500, 40));

                String medal = (rank == 1) ? "🥇 " : (rank == 2) ? "🥈 " : (rank == 3) ? "🥉 " : rank + ". ";

                JLabel nameLabel = new JLabel(medal + entry.getKey());
                nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
                nameLabel.setForeground(Color.WHITE);

                JLabel scoreLabel = new JLabel(entry.getValue() + " Wins");
                scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
                scoreLabel.setForeground(new Color(46, 204, 113));

                row.add(nameLabel, BorderLayout.WEST);
                row.add(scoreLabel, BorderLayout.EAST);

                historyListPanel.add(row);
                historyListPanel.add(Box.createVerticalStrut(10));
                rank++;
            }
        }
        historyListPanel.revalidate();
        historyListPanel.repaint();
    }

    private void showWinnerRank() {
        List<Player> rankedPlayers = new ArrayList<>(players);
        rankedPlayers.sort(Comparator.comparingInt(Player::getPoint).reversed());

        winnerDisplayPanel.removeAll();

        JLabel rankTitle = new JLabel("Ranking Akhir Berdasarkan Poin:");
        rankTitle.setFont(new Font("Arial", Font.BOLD, 24));
        rankTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        winnerDisplayPanel.add(rankTitle);
        winnerDisplayPanel.add(Box.createVerticalStrut(20));

        if (sound != null) {
            sound.stopBacksound();
            sound.playSoundEffect("assets/win.wav");
        }

        // Simpan pemenang ke history
        if (!rankedPlayers.isEmpty()) {
            Player champion = rankedPlayers.get(0);
            historyManager.addWin(champion.getName());
        }

        for (int i = 0; i < rankedPlayers.size(); i++) {
            Player p = rankedPlayers.get(i);
            int rank = i + 1;
            String trophy = "";
            if (rank == 1) {
                trophy = "1";
                JOptionPane.showMessageDialog(this, " SELAMAT KEPADA " + p.getName() + " sebagai Juara Pertama!", "Pemenang!", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (rank == 2) trophy = "2";
            else if (rank == 3) trophy = "3";

            JLabel rankLabel = new JLabel(trophy + " Peringkat " + rank + ": " + p.getName() + " dengan Poin: " + p.getPoint());
            rankLabel.setFont(new Font("Arial", Font.PLAIN, 20));
            rankLabel.setForeground(Color.decode(p.getColor()));
            rankLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            winnerDisplayPanel.add(rankLabel);
            winnerDisplayPanel.add(Box.createVerticalStrut(10));
        }
        winnerDisplayPanel.revalidate();
        winnerDisplayPanel.repaint();
        cardLayout.show(mainPanel, "winner");
    }

    private void resetGame() {
        gameState = "menu";
        players.clear();
        playerQueue = new PlayerQueue();
        currentPlayer = null;
        diceResult = null;
        initializeLinks();
        initializeNodePoints();

        if (sound != null) {
            sound.stopBacksound();
            sound.playBacksound("assets/background_music.wav");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SnakesLaddersGame game = new SnakesLaddersGame();
            game.setVisible(true);
        });
    }
}