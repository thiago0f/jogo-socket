import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 2;
    
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private char[][] board;
    private char currentPlayer;
    private boolean gameActive;
    private int connectedPlayers;
    
    public Servidor() {
        clients = new ArrayList<>();
        board = new char[3][3];
        currentPlayer = 'X';
        gameActive = false;
        connectedPlayers = 0;
        initializeBoard();
    }
    
    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }
    
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado na porta " + PORT);
            
            while (true) {
                if (connectedPlayers < MAX_PLAYERS) {
                    Socket clientSocket = serverSocket.accept();
                    connectedPlayers++;
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this, connectedPlayers);
                    clients.add(clientHandler);
                    
                    // Assign symbol to player
                    char symbol = (connectedPlayers == 1) ? 'X' : 'O';
                    clientHandler.setSymbol(symbol);
                    
                    new Thread(clientHandler).start();
                    
                    System.out.println("Jogador conectado. Total de jogadores: " + connectedPlayers);
                    
                    // Start game when we have 2 players
                    if (connectedPlayers == 2) {
                        startGame();
                    }
                } else {
                    // Server full - reject connection
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("SERVIDOR_CHEIO|O servidor está cheio. Tente novamente mais tarde.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        } finally {
            stopServer();
        }
    }
    
    private synchronized void startGame() {
        if (!gameActive && clients.size() >= 2) {
            gameActive = true;
            initializeBoard();
            currentPlayer = 'X';
            broadcastMessage("JOGO_INICIADO|O jogo começou! Jogador X começa.");
            sendBoardState();
        }
    }
    
    public synchronized void processMove(int playerId, int row, int col) {
        if (!gameActive) return;
        
        ClientHandler currentPlayerHandler = getCurrentPlayerHandler();
        if (currentPlayerHandler == null || currentPlayerHandler.getPlayerId() != playerId) {
            // Not current player's turn
            getClientHandlerById(playerId).sendMessage("RESULTADO|Não é sua vez!");
            return;
        }
        
        if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != '-') {
            // Invalid move
            currentPlayerHandler.sendMessage("RESULTADO|Jogada inválida! Tente novamente.");
            return;
        }
        
        // Make the move
        board[row][col] = currentPlayer;
        broadcastMessage("MOVIMENTO|" + playerId + "|" + row + "|" + col + "|" + currentPlayer);
        sendBoardState();
        
        // Check for win or draw
        if (checkWin(currentPlayer)) {
            broadcastMessage("FIM_JOGO|Vitória do jogador " + currentPlayer + "!");
            resetGame();
        } else if (isBoardFull()) {
            broadcastMessage("FIM_JOGO|Empate! O tabuleiro está cheio.");
            resetGame();
        } else {
            // Switch player
            switchPlayer();
            broadcastMessage("TURNO|" + getCurrentPlayerHandler().getPlayerId());
        }
    }
    
    private ClientHandler getCurrentPlayerHandler() {
        for (ClientHandler handler : clients) {
            if (handler.getSymbol() == currentPlayer) {
                return handler;
            }
        }
        return null;
    }
    
    private ClientHandler getClientHandlerById(int playerId) {
        for (ClientHandler handler : clients) {
            if (handler.getPlayerId() == playerId) {
                return handler;
            }
        }
        return null;
    }
    
    private void switchPlayer() {
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
    }
    
    private boolean checkWin(char player) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }
        }
        
        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                return true;
            }
        }
        
        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }
        
        return false;
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '-') {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void sendBoardState() {
        StringBuilder boardStr = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardStr.append(board[i][j]);
                if (j < 2) boardStr.append(",");
            }
            if (i < 2) boardStr.append("|");
        }
        broadcastMessage("ESTADO|" + boardStr.toString());
    }
    
    public synchronized void broadcastMessage(String message) {
        Iterator<ClientHandler> iterator = clients.iterator();
        while (iterator.hasNext()) {
            ClientHandler client = iterator.next();
            if (!client.sendMessage(message)) {
                // Client disconnected
                iterator.remove();
                connectedPlayers--;
            }
        }
    }
    
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        connectedPlayers--;
        if (gameActive) {
            broadcastMessage("JOGADOR_SAIU|Um jogador saiu. Jogo interrompido.");
            gameActive = false;
        }
        System.out.println("Jogador desconectado. Total de jogadores: " + connectedPlayers);
    }
    
    private void resetGame() {
        gameActive = false;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Only restart if we still have 2 players
                if (connectedPlayers >= 2 && clients.size() >= 2) {
                    startGame();
                }
            }
        }, 5000); // Restart after 5 seconds
    }
    
    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        Servidor server = new Servidor();
        server.startServer();
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private Servidor server;
    private BufferedReader in;
    private PrintWriter out;
    private int playerId;
    private char symbol;
    
    public ClientHandler(Socket socket, Servidor server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }
    
    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }
    
    public char getSymbol() {
        return symbol;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Send player symbol
            out.println("SIMBOLO|" + symbol);
            out.println("ID_JOGADOR|" + playerId);
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Jogador " + playerId + " desconectado: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 1) return;
        
        String command = parts[0];
        
        switch (command) {
            case "JOGADA":
                if (parts.length >= 3) {
                    try {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        server.processMove(playerId, row, col);
                    } catch (NumberFormatException e) {
                        sendMessage("RESULTADO|Formato de jogada inválido!");
                    }
                }
                break;
            case "CHAT":
                if (parts.length >= 2) {
                    server.broadcastMessage("CHAT|" + playerId + ": " + parts[1]);
                }
                break;
            default:
                sendMessage("RESULTADO|Comando desconhecido: " + command);
                break;
        }
    }
    
    public boolean sendMessage(String message) {
        try {
            out.println(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void closeConnection() {
        try {
            server.removeClient(this);
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão com o cliente: " + e.getMessage());
        }
    }
}