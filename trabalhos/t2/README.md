# Trabalho 2 de Sistemas Distribuídos (ELC1018)

Implementa um *middleware* de multicast causal: mensagens enviadas a um grupo
de processos são entregues à aplicação respeitando a ordem causal,
independentemente da ordem em que chegam pela rede. Enquanto não podem ser
entregues, as mensagens ficam em um buffer, de onde são descartadas apenas
quando se tornam estáveis, isto é, recebidas por todos os participantes.

## Estrutura

Projeto Gradle com dois módulos: 

- `lib/` (o *middleware*).
- `app/` (o cliente interativo de terminal).

## Execução

Em cada terminal, inicie um participante com seu endereço e porta:

```bash
./gradlew :app:run --args="127.0.0.1 5000" --console=plain -q
./gradlew :app:run --args="127.0.0.1 5001" --console=plain -q
```

### Comandos

| Comando         | Descrição                                       |
| --------------- | ----------------------------------------------- |
| `/enviar <msg>` | envia uma mensagem para o grupo                 |
| `/liberar <ids>` | libera transmissões retidas (ex.: `1` ou `1-3`) |
| `/retidas`      | lista as transmissões retidas                   |
| `/ajuda`        | mostra a ajuda                                  |
| `/sair`         | encerra o cliente                               |

As transmissões são sempre retidas e podem ser liberadas manualmente em qualquer
ordem, permitindo forçar reordenações na rede e demonstrar que a entrega continua
causal.

## Relógios

Cada mensagem carrega o relógio vetorial do remetente no
momento do envio, ou seja, suas dependências causais. Cada processo mantém uma
matriz de relógios, em que  a linha `MC[self]` registra o que ele
próprio já recebeu de cada processo, e as demais linhas registram o que ele
sabe sobre o conhecimento dos outros (atualizado pelos relógios que chegam nas
mensagens).

## Ordenação causal

Uma mensagem recebida vai para um *buffer* e só é entregue quando seus
requisitos causais estão satisfeitos. Isso ocorre quando para uma mensagem `m`
de um remetente `S`:

1. **É a próxima de `S`:** `m.VC[S] == MC[self][S] + 1`, ou seja, nenhuma mensagem
   anterior de `S` ficou faltando.
2. **As dependências já chegaram:** para todo processo `X` (diferente de `S`),
   `m.VC[X] <= MC[self][X]`, indicando que tudo que `S` havia recebido (e que
   portanto pode ter causado `m`) já foi recebido também.

Enquanto qualquer condição falhar, `m` permanece no buffer. A entrega é
reavaliada a cada nova mensagem.

## Estabilidade

Uma mensagem é estável quando todos os participantes já a receberam
e, por isso, pode ser descartada do *buffer*. Isso vale para a mensagem `m` de
um remetente `S` quando, para todo participante `X`:

```
m.VC[S] <= MC[X][S]
```

Ou seja, a linha de cada `X` na matriz indica que ele já viu a mensagem. 
