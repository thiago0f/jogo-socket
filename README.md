# Jogo da Velha Multiplayer

Este é um jogo da velha multiplayer implementado em Java utilizando sockets TCP/IP.

## Estrutura do Projeto

- `Servidor.java`: Implementação do servidor que gerencia as conexões e a lógica do jogo
- `Cliente.java`: Implementação do cliente que permite aos jogadores interagir com o jogo

## Requisitos

- Java 8 ou superior

## Como Executar

### 1. Compilar os arquivos

Abra um terminal na pasta do projeto e execute:

```bash
javac Servidor.java
javac Cliente.java
```

### 2. Iniciar o Servidor

```bash
java Servidor
```

O servidor será iniciado na porta 12345 e mostrará a mensagem:
```
Servidor iniciado na porta 12345
```

### 3. Iniciar os Clientes

Abra dois terminais separados (um para cada jogador) e execute em cada um:

```bash
java Cliente
```

Cada cliente tentará se conectar ao servidor em `localhost:12345`.

## Como Jogar

1. O primeiro cliente a se conectar será o Jogador X
2. O segundo cliente a se conectar será o Jogador O
3. O jogo começará automaticamente quando ambos os jogadores estiverem conectados
4. O tabuleiro será exibido em ambos os clientes

### Fazer uma Jogada

- Digite a posição desejada no formato `linha,coluna` (valores de 0 a 2)
- Exemplo: `1,2` para jogar na linha 1, coluna 2

### Chat

- Envie mensagens no chat digitando `chat:sua mensagem aqui`
- Exemplo: `chat:Olá, vamos jogar!`

### Sair do Jogo

- Digite `sair` para desconectar do servidor

## Protocolo de Comunicação

O jogo utiliza os seguintes tipos de mensagens:

- `JOGADA|linha|coluna` - Envia uma jogada para o servidor
- `RESULTADO|mensagem` - Resultado de uma ação
- `CHAT|mensagem` - Mensagem de chat
- `ESTADO|estado_do_tabuleiro` - Estado atual do tabuleiro
- `TURNO|id_jogador` - Indica de quem é a vez
- `FIM_JOGO|mensagem` - Anuncia o fim do jogo
- `SIMBOLO|X ou O` - Informa o símbolo do jogador
- `ID_JOGADOR|id` - Informa o ID do jogador

## Funcionalidades

### Servidor
- Aceita conexões de múltiplos clientes (mínimo 2, máximo 2)
- Gerencia o estado global do jogo
- Valida todas as jogadas
- Notifica clientes sobre atualizações
- Processa comandos e envia respostas
- Gerencia desconexões sem travar
- Reinicia o jogo automaticamente após o fim de uma partida

### Cliente
- Conecta ao servidor via IP/porta
- Envia jogadas e comandos
- Recebe e exibe o estado do jogo
- Interface de usuário textual
- Mostra mensagens de chat
- Indica de quem é a vez de jogar

## Tratamento de Erros

- Conexão perdida: O jogo é interrompido e os clientes são notificados
- Jogada inválida: O jogador recebe uma mensagem de erro
- Servidor cheio: Novas conexões são rejeitadas com uma mensagem

## Testando o Jogo

Para testar o jogo, siga estas etapas:

1. **Inicie o servidor** em um terminal:
   ```bash
   java Servidor
   ```

2. **Inicie o primeiro cliente** em outro terminal:
   ```bash
   java Cliente
   ```
   Este será o Jogador X.

3. **Inicie o segundo cliente** em um terceiro terminal:
   ```bash
   java Cliente
   ```
   Este será o Jogador O.

4. **Jogue o jogo**:
   - O jogo começará automaticamente quando ambos os jogadores estiverem conectados
   - O Jogador X começa sempre
   - Faça jogadas alternando entre os clientes usando o formato `linha,coluna` (0-2)
   - Experimente funcionalidades como chat e desconexão para verificar o tratamento de erros

Ambos os arquivos Java foram compilados com sucesso, então o jogo está pronto para ser executado.
