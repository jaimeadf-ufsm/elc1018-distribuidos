# Chat distribuído com gRPC e Java

Integrantes: Jaime Antonio Daniel Filho e Diego Ribeiro Chaves

Este projeto contém a implementação de uma aplicação distribuída de Chat com suporte a uma única sala. O projeto foi desenvolvido na linguagem Java, utilizando comunicação gRPC e contratos definidos via Protocol Buffers. A arquitetura é dividida em Servidor de Chat e em Cliente de Chat.

## Requisitos implementados
A aplicação atende a todos os requisitos funcionais absolutos especificados para o trabalho:
- **Registro de Usuários (RFA01):** Os clientes se registram com um nome único através de uma chamada unária (```Register```). O servidor valida e rejeita nomes duplicados.
- **Sala Única (RFA02):** O servidor gerencia e centraliza uma única sala que suporta múltiplos usuários simultâneos.
- **Envio e Broadcast de Mensagens (RFA03 e RFA04):** Mensagens são enviadas via chamada unária (```SendMessage```) e contêm o remetente, conteúdo e data/hora.
- **Streaming de Mensagens (RFA05 e RFA06):** O recebimento de mensagens ocorre de forma contínua através de uma conexão ativa via Server Streaming (```ReceiveMessages```). A ordem das mensagens enviadas por um mesmo remetente é preservada.
- **Notificações do Sistema (RFA07):** A aplicação emite notificações para todos os membros ativos sempre que um usuário entra ou sai da sala.

## Guia de execução

Este guia descreve como compilar e executar o trabalho, por meio das tarefas definidas no Gradle.

### Compilação do projeto

Antes de iniciar o servidor ou os clientes, é necessário compilar o código-fonte:

```bash
.\gradlew.bat build
```

### Execução do servidor

A tarefa `runServer` inicia o servidor do chat, que ficará aguardando as conexões dos clientes.

#### Parâmetros
- `-Pport=<porta>`: Especifica a porta em que o servidor irá ouvir as conexões. Se este parâmetro for omitido, a porta `50051` será utilizada como padrão.

#### Exemplos

```bash
.\gradlew.bat runServerc
```

### Execução do cliente

A tarefa `runClient` roda a aplicação do usuário, estabelecendo a conexão com um servidor.

#### Parâmetros

- `-Pcommand=<comando>`: Define a ação do cliente, sendo os seguintes valores válidos:
  - `register`: Registra um nome de usuário no servidor e, em seguida, conecta-se ao chat.
  - `join`: Conecta-se ao chat utilizando um nome de usuário.
- `-Phost=<endereço>`: Especifica o IP e a porta do servidor de destino.
- `-Pusername=<nome>`: Especifica o nome de usuário que será utilizado.

#### Exemplos

```bash
.\gradlew.bat runClient -Pcommand=register -Phost=localhost:50051 -Pusername=jaime
```

```bash
.\gradlew.bat runClient -Pcommand=register -Phost=localhost:50051 -Pusername=diego
```

```bash
.\gradlew.bat runClient -Pcommand=join -Phost=localhost:50051 -Pusername=jaime
```
