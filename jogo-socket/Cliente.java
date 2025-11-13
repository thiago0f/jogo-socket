import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private char playerSymbol;
    private int playerId;
    private char[][] board;
    private boolean myTurn;
    
    public Cliente() {
        scanner = new Scanner(System.in);
        board = new char[3][3];
        initializeBoard();
    }
    
    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }
    
    public void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("Conectado ao servidor!");
            
            // Start listening thread
            Thread listenerThread = new Thread(new ServerListener());
            listenerThread.start();
            
            // Start input loop
            inputLoop();
            
        } catch (IOException e) {
            System.err.println("Erro ao conectar ao servidor: " + e.getMessage());
        }
    }
    
    private void inputLoop() {
        System.out.println("Digite suas jogadas no formato: linha,coluna (0-2)");
        System.out.println("Ou digite 'chat:mensagem' para enviar uma mensagem no chat");
        System.out.println("Digite 'sair' para sair do jogo");
        
        String input;
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine();
            
            if ("sair".equalsIgnoreCase(input)) {
                break;
            } else if (input.startsWith("chat:")) {
                String message = input.substring(5);
                out.println("CHAT|" + message);
            } else {
                // Try to parse as a move
                String[] parts = input.split(",");
                if (parts.length == 2) {
                    try {
                        int row = Integer.parseInt(parts[0].trim());
                        int col = Integer.parseInt(parts[1].trim());
                        
                        if (!myTurn) {
                            System.out.println("Aguarde sua vez!");
                        } else {
                            out.println("JOGADA|" + row + "|" + col);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Formato inválido. Use: linha,coluna (ex: 1,2)");
                    }
                } else {
                    System.out.println("Comando inválido. Use 'linha,coluna', 'chat:mensagem' ou 'sair'");
                }
            }
        }
        
        disconnect();
    }
    
    private void updateBoard(String boardData) {
        String[] rows = boardData.split("\\|");
        for (int i = 0; i < rows.length && i < 3; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < cells.length && j < 3; j++) {
                board[i][j] = cells[j].charAt(0);
            }
        }
        printBoard();
    }
    
    private void printBoard() {
        System.out.println("\n--- Tabuleiro ---");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("-----------------\n");
    }
    
    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
            System.out.println("Desconectado do servidor.");
        } catch (IOException e) {
            System.err.println("Erro ao desconectar: " + e.getMessage());
        }
    }
    
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    processServerMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Erro na conexão com o servidor: " + e.getMessage());
            } finally {
                disconnect();
            }
        }
        
        private void processServerMessage(String message) {
            String[] parts = message.split("\\|");
            if (parts.length < 1) return;
            
            String command = parts[0];
            
            switch (command) {
                case "SIMBOLO":
                    if (parts.length >= 2) {
                        playerSymbol = parts[1].charAt(0);
                        System.out.println("Você é o jogador " + playerSymbol);
                    }
                    break;
                    
                case "ID_JOGADOR":
                    if (parts.length >= 2) {
                        playerId = Integer.parseInt(parts[1]);
                        System.out.println("Seu ID de jogador: " + playerId);
                    }
                    break;
                    
                case "JOGO_INICIADO":
                    if (parts.length >= 2) {
                        System.out.println(parts[1]);
                        myTurn = (playerSymbol == 'X');
                        if (myTurn) {
                            System.out.println("Sua vez! Faça sua jogada.");
                        } else {
                            System.out.println("Aguarde o outro jogador.");
                        }
                    }
                    break;
                    
                case "ESTADO":
                    if (parts.length >= 2) {
                        updateBoard(parts[1]);
                    }
                    break;
                    
                case "TURNO":
                    if (parts.length >= 2) {
                        int currentPlayerId = Integer.parseInt(parts[1]);
                        myTurn = (currentPlayerId == playerId);
                        if (myTurn) {
                            System.out.println("Sua vez! Faça sua jogada.");
                        } else {
                            System.out.println("Aguardando o outro jogador...");
                        }
                    }
                    break;
                    
                case "RESULTADO":
                    if (parts.length >= 2) {
                        System.out.println("Resultado: " + parts[1]);
                    }
                    break;
                    
                case "MOVIMENTO":
                    if (parts.length >= 5) {
                        int movePlayerId = Integer.parseInt(parts[1]);
                        int row = Integer.parseInt(parts[2]);
                        int col = Integer.parseInt(parts[3]);
                        char symbol = parts[4].charAt(0);
                        System.out.println("Jogador " + movePlayerId + " (" + symbol + ") jogou na posição (" + row + "," + col + ")");
                    }
                    break;
                    
                case "CHAT":
                    if (parts.length >= 2) {
                        System.out.println("[Chat] " + parts[1]);
                    }
                    break;
                    
                case "FIM_JOGO":
                    if (parts.length >= 2) {
                        System.out.println("=== FIM DE JOGO ===");
                        System.out.println(parts[1]);
                        System.out.println("====================");
                    }
                    break;
                    
                case "JOGADOR_SAIU":
                    if (parts.length >= 2) {
                        System.out.println("Aviso: " + parts[1]);
                    }
                    break;
                    
                case "SERVIDOR_CHEIO":
                    if (parts.length >= 2) {
                        System.out.println("Erro: " + parts[1]);
                        System.exit(0);
                    }
                    break;
                    
                default:
                    System.out.println("Mensagem do servidor: " + message);
                    break;
            }
        }
    }
    
    public static void main(String[] args) {
        Cliente client = new Cliente();
        client.connectToServer();
    }
}