# Chat Distribuído com gRPC e Java
Este repositório contém a implementação de uma aplicação distribuída de Chat com suporte a uma única sala. O projeto foi desenvolvido na linguagem Java, utilizando comunicação gRPC e contratos definidos via Protocol Buffers. A arquitetura é dividida em dois processos principais: o Servidor de Chat e o Cliente de Chat.

## O que foi implementado
A aplicação atende a todos os requisitos funcionais absolutos especificados para o trabalho:
- **Registro de Usuários (RFA01):** Os clientes se registram com um nome único através de uma chamada unária (```Register```). O servidor valida e rejeita nomes duplicados.
- **Sala Única (RFA02):** O servidor gerencia e centraliza uma única sala que suporta múltiplos usuários simultâneos.
- **Envio e Broadcast de Mensagens (RFA03 e RFA04):** Mensagens são enviadas via chamada unária (```SendMessage```) e contêm o remetente, conteúdo e carimbo de data/hora (*timestamp*).
- **Streaming de Mensagens (RFA05 e RFA06):** O recebimento de mensagens ocorre de forma contínua através de uma conexão ativa via *Server Streaming* (```ReceiveMessages```). A ordem das mensagens enviadas por um mesmo remetente é preservada.
- **Notificações do Sistema (RFA07):** A aplicação emite alertas (eventos de notificação) para todos os membros ativos sempre que um usuário entra ou sai da sala.
- **Restrições Respeitadas:** A comunicação ocorre exclusivamente via gRPC, sem o uso direto de sockets, REST ou bibliotecas de chat externas.

## Arquitetura e decisões de implementação
Aplicações gRPC são naturalmente multithread: o servidor processa múltiplas requisições de clientes simultaneamente. Por isso, a aplicação foi desenhada com atenção especial à sincronização.

### Sincronização e Condições de Corrida
Para evitar que o estado interno do servidor seja corrompido quando vários clientes enviam mensagens ou tentam se registrar ao mesmo tempo, as seguintes estratégias foram adotadas:
- **Métodos ```synchronized``` no Servidor:** A classe ```ChatRoom``` (que encapsula a lógica de negócio) utiliza o modificador ```synchronized``` em todas as suas operações críticas, como ```register```, ```unregister```, ```connect```, ```disconnect``` e ```broadcast``. Isso garante que apenas uma thread modifique ou leia os mapas de usuários e o histórico de mensagens por vez.
- **Coleções Seguras para Threads:** A classe ```UserChannel``` utiliza uma ```Collections.synchronizedList``` para armazenar os fluxos de rede (*streams*). Isso previne exceções de concorrência se um usuário se conectar ou desconectar enquanto uma mensagem está sendo transmitida.

### Lógica de Conexão e Desconexão
- **Desligamento Gracioso (*Graceful Shutdown*):** O servidor possui um *Shutdown Hook* que intercepta sinais de encerramento do sistema (como ```Ctrl+C```) e aguarda até 30 segundos para fechar as conexões gRPC de forma limpa, garantindo que recursos da rede não fiquem presos.
- **Detecção de Quedas:** No servidor, o método ```setOnCancelHandler``` é acionado automaticamente se o cliente perder a conexão de forma abrupta, acionando a lógica de desconexão e limpeza de estado.

### Responsividade no Cliente
O cliente interage com a rede sem travar a interface do usuário:

- **Stubs Assíncronos vs. Bloqueantes:** O registro e o envio de mensagens utilizam chamadas bloqueantes (*Blocking Stub*), pois dependem de uma resposta imediata de sucesso/falha do servidor. Já o recebimento de mensagens utiliza um *Async Stub*, permitindo que o cliente receba mensagens em segundo plano.
- **I/O Não Bloqueante:** A leitura do teclado utiliza ```System.in.available()``` e um ```CountDownLatch```. Isso impede que a thread principal fique travada esperando o usuário digitar, permitindo que a aplicação processe comandos de saída (```/sair```) e encerre corretamente ao perder a conexão com o servidor.

## Como executar
Para executar os comandos abaixo, certifique-se de que o projeto foi compilado corretamente e que as dependências do gRPC/Protobuf estão no seu ```classpath```.

### 1. Iniciando o Servidor

O servidor inicia por padrão na porta ```50051```. Você pode passar uma porta diferente como argumento, se desejar.

**Comando:**
```bash
java elc1018.grpc.chat.server.ChatServer [porta_opcional]
```

### 2. Iniciando o Cliente

Para conectar um cliente, você precisa informar a ação (registrar ou apenas entrar), o endereço do servidor (host + porta) e o nome de usuário desejado. O cliente aceita três argumentos obrigatórios nesta ordem: `<comando> <host> <username>`.

**Comandos:**

Para registrar um novo usuário e entrar automaticamente na sala:
```bash
java elc1018.grpc.chat.client.ChatClient register localhost:50051 "Alice"
```

Para entrar na sala com um usuário que (hipoteticamente) já está registrado, embora a regra padrão da sala os remova ao sair:
```bash
java elc1018.grpc.chat.client.ChatClient join localhost:50051 "Alice"
```

### 3. Interagindo no Chat

* Após conectar, basta digitar qualquer texto no terminal e pressionar `ENTER` para enviar a mensagem para a sala.
* Para fechar o cliente de forma segura, digite o comando reservado:
  ```text
  /sair
  ```