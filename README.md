# Monitoramento de Açudes do Semiárido

**Aplicação Java 17 com RethinkDB 2.4 — banco NoSQL orientado a documentos**

> Projeto acadêmico para demonstrar como armazenar e acompanhar níveis de reservatórios, manter o histórico de medições e transmitir atualizações em tempo real.

## Identificação do projeto

- **Problema:** acompanhar níveis de açudes e consultar seu histórico em um cenário de estiagem.
- **Aplicação:** cadastro de reservatórios, registro de medições, consulta de níveis críticos e monitoramento de mudanças.
- **Banco escolhido:** RethinkDB 2.4, modelo de documentos.
- **Equipe:** Kauã Lopes // Rafael Lima // Vitor Miguel
- **Tecnologias:** Java 17, Maven, driver Java do RethinkDB e Docker Compose.

### Texto para registrar a equipe

> Nossa equipe desenvolverá um sistema de monitoramento de açudes do Semiárido, com cadastro de reservatórios, registro do histórico de níveis e acompanhamento de alertas. Usaremos o RethinkDB, banco NoSQL orientado a documentos com consultas ReQL e changefeeds para notificar clientes sobre alterações em tempo real. Integrantes: **[inserir nomes]**.

## 1. Objetivo e escopo

O sistema organiza informações de reservatórios em documentos e permite que uma pessoa operadora registre medições percentuais. A aplicação mantém o nível atual e o histórico, classifica os níveis como normal, atenção ou crítico, calcula médias e publica alterações por meio de um changefeed.

O projeto é uma aplicação de linha de comando para fins acadêmicos. A interface em `localhost:8080` é o painel administrativo do RethinkDB, usado para inspecionar o banco e executar ReQL; ela não é um painel próprio da aplicação.

## 2. Preparação e execução

### Requisitos

- Docker com Docker Compose v2
- JDK 17 ou superior
- Maven

Na pasta do projeto, inicie o banco, compile a aplicação e crie o schema:

```powershell
docker compose up -d
mvn -q compile
mvn -q exec:java "-Dexec.args=init"
```

O banco fica disponível para a aplicação em `localhost:28015`; o painel administrativo fica em [http://localhost:8080](http://localhost:8080). O Docker Compose cria volumes persistentes para os dados dos nós.

### Menu interativo

Inicie a aplicação sem argumentos para abrir o menu CRUD:

```powershell
mvn -q exec:java
```

Na primeira execução, escolha **1 — Inicializar banco de dados**. Depois use o menu para cadastrar, consultar, atualizar e remover reservatórios, acompanhar leituras, exportar backups ou restaurar um arquivo. A opção **0** encerra o programa. As opções exibem uma animação curta de carregamento na transição; o menu usa cores quando o terminal oferece suporte a ANSI. Defina `NO_COLOR` para desativar as cores.

### Comandos diretos

Os comandos por argumento continuam disponíveis; cada um é executado em uma nova invocação:

```powershell
mvn -q exec:java "-Dexec.args=add 'Epitácio Pessoa' Boqueirão 411 35"
mvn -q exec:java "-Dexec.args=add 'Açude Cordeiro' Serra_Branca 69 60"
mvn -q exec:java "-Dexec.args=list"
mvn -q exec:java "-Dexec.args=get <id>"
mvn -q exec:java "-Dexec.args=leitura <id> 28.5"
mvn -q exec:java "-Dexec.args=historico <id>"
mvn -q exec:java "-Dexec.args=delete <id>"
```

Substitua `<id>` pelo identificador exibido ao cadastrar o reservatório. Para ver todos os comandos diretos, execute `mvn -q exec:java "-Dexec.args=help"`.

### Demonstração de changefeed

Cadastre pelo menos um reservatório. Em seguida, abra dois terminais na pasta do projeto:

```powershell
# Terminal 1: recebe alterações em tempo real; digite q e Enter para sair do feed
mvn -q exec:java "-Dexec.args=watch"
```

```powershell
# Terminal 2: altera os níveis cadastrados em intervalos de dois segundos
mvn -q exec:java "-Dexec.args=simulate <id>"
```

A simulação pede o ID do reservatório e altera temporariamente apenas esse açude para demonstrar o changefeed. Mostra cada mudança de nível, para quando chega a 0% e restaura o nível inicial ao final; não grava as leituras simuladas no histórico nem altera o timestamp operacional. Se não secar, pode durar até cerca de 18 segundos. No menu, retorna automaticamente à lista de opções.

Também é possível usar o comando `leitura <id> <nivel_pct>` no segundo terminal para observar uma alteração individual.

## 3. Modelo de dados

Os dados ficam em duas tabelas. Cada linha é um documento JSON com identificador primário `id`.

**`reservatorios`** — estado atual do açude:

```json
{
  "id": "identificador-gerado-pelo-banco",
  "nome": "Epitácio Pessoa",
  "municipio": "Boqueirão",
  "capacidade_hm3": 411.0,
  "nivel_pct": 35.0,
  "criado_em": 1730000000000,
  "atualizado_em": 1730000000000
}
```

**`leituras`** — medições históricas, relacionadas ao reservatório pelo campo `reservatorio_id`:

```json
{
  "id": "identificador-gerado-pelo-banco",
  "reservatorio_id": "identificador-do-reservatorio",
  "nivel_pct": 28.5,
  "ts": 1730000000000
}
```

O campo `ts` registra o instante da medição em milissegundos desde a época Unix. O schema cria um índice secundário em `leituras.reservatorio_id` para buscar o histórico de um reservatório.

## 4. CRUD e consultas

| Operação | Comando | Efeito |
|---|---|---|
| Create | `add <nome> <município> <capacidade_hm3> <nível_pct>` | Cadastra um reservatório e retorna seu ID. |
| Read | `list`, `get <id>`, `municipio <nome>` | Lista, consulta por ID ou filtra por município. |
| Update | `leitura <id> <nível_pct>` | Atualiza o nível atual e grava uma leitura histórica. |
| Delete | `delete <id>` | Remove o reservatório e suas leituras relacionadas. |
| Consulta | `criticos <limite>`, `media`, `historico <id>` | Filtra níveis, calcula a média e consulta o histórico. |

O filtro `criticos <limite>` inclui níveis iguais ao limite e abaixo dele; portanto, `criticos 0` lista reservatórios exatamente em 0%. As regras de negócio validam campos obrigatórios, capacidade positiva e níveis entre 0% e 100%. Os níveis recebem uma classificação: crítico abaixo de 20%, atenção de 20% a menos de 40% e normal a partir de 40%.

## 5. RethinkDB: características e aplicações

- **Documentos JSON:** os documentos podem conter campos diferentes sem exigir uma definição rígida de colunas para toda a tabela.
- **ReQL:** linguagem de consulta encadeável, disponível nos drivers do banco; permite filtros, agregações, junções e operações de escrita.
- **Changefeeds:** uma consulta pode acompanhar alterações e enviar novos valores aos clientes sem que a aplicação consulte repetidamente a tabela. Este projeto usa `changes()` na tabela `reservatorios`.
- **Índices secundários:** aceleram consultas por campos além da chave primária, com custo adicional de armazenamento e escrita.
- **Escala horizontal:** o banco permite distribuir tabelas em shards e configurar réplicas por tabela.
- **Painel administrativo:** permite inspecionar tabelas, índices e configuração do cluster e executar ReQL.

Aplicações que podem se beneficiar desses recursos incluem painéis em tempo real, telemetria e IoT, monitoramento operacional, jogos multiplayer e sistemas colaborativos. O projeto explora especificamente o caso de monitoramento de recursos hídricos.

### Exemplo ReQL no painel

Para listar reservatórios abaixo de 40%:

```javascript
r.db('acudes').table('reservatorios')
  .filter(r.row('nivel_pct').lt(40))
```

### Pontos fortes e limitações

**Pontos fortes**

- Changefeeds nativos facilitam a propagação de alterações em tempo real.
- A estrutura de documentos é adequada a dados com campos que podem evoluir.
- ReQL reúne consultas e operações de escrita numa API encadeável.
- Índices, shards e réplicas são configuráveis por tabela.

**Limitações e cuidados**

- Índices secundários consomem espaço e podem reduzir a velocidade de escrita.
- A configuração de cluster exige atenção a réplicas, quórum e disponibilidade.
- A empresa original encerrou as operações em 2016; antes de uma adoção comercial, é importante avaliar o suporte, a atividade do ecossistema e os requisitos de manutenção. Consulte o histórico do projeto na [Database of Databases](https://dbdb.io/db/rethinkdb/).
- O changefeed deste exemplo mostra alterações no terminal. Uma aplicação em produção precisaria definir autenticação, interface, autorização e estratégia de entrega das notificações.

## 6. Redundância e disponibilidade

O `docker-compose.yml` deste projeto inicia dois nós e mantém os dados em volumes separados. Para configurar duas réplicas da tabela pelo Data Explorer, use:

```javascript
r.db('acudes').table('reservatorios')
  .reconfigure({shards: 1, replicas: 2})
```

Replicação mantém cópias dos dados em mais de um nó, mas não substitui backup. **Este ambiente de dois nós demonstra replicação, mas não garante failover automático:** a documentação do RethinkDB requer pelo menos três servidores, três réplicas da tabela e uma maioria das réplicas disponíveis para o failover automático. Assim, a perda de um nó num cluster de dois pode interromper as escritas até que a situação seja corrigida.

## 7. Backup e restauração

### Backup JSON da aplicação

O comando da aplicação exporta documentos das duas tabelas para um arquivo JSON:

```powershell
mvn -q exec:java "-Dexec.args=backup backup-acudes.json"
# Em um banco novo, crie as tabelas antes de restaurar o JSON:
mvn -q exec:java "-Dexec.args=init"
mvn -q exec:java "-Dexec.args=restore backup-acudes.json"
```

### Dump nativo do RethinkDB

A ferramenta oficial `rethinkdb dump` cria um arquivo compactado que inclui documentos e metadados das tabelas. Com o driver Python instalado:

```powershell
python -m pip install rethinkdb
rethinkdb dump -c localhost:28015 -f backup-acudes.tar.gz
rethinkdb restore backup-acudes.tar.gz -c localhost:28015
```

O projeto **não agenda backups automaticamente**. Para uma rotina automática, agende o comando de dump com o Agendador de Tarefas do Windows ou `cron`, defina retenção e local seguro para as cópias e teste a restauração periodicamente. Replicação ajuda na disponibilidade; backup protege contra exclusão acidental, corrupção e outros incidentes que podem atingir as réplicas.

## 8. Desempenho e configuração

- O histórico usa o índice `reservatorio_id` com `getAll`, evitando filtrar toda a tabela de leituras para cada reservatório.
- Consultas por município e níveis críticos usam filtros gerais. Em bases maiores, avalie índices adequados a essas consultas, lembrando do custo em armazenamento e escrita.
- O tamanho do cache de páginas pode ser configurado para o hardware e a carga do servidor. O RethinkDB escolhe um valor padrão automaticamente; ajuste-o somente após observar o uso de memória e o comportamento das consultas.
- A durabilidade padrão `hard` confirma as escritas depois de persistidas em disco. O modo `soft` pode responder mais rápido, mas reduz a resiliência a falhas; este projeto mantém o padrão seguro.

## 9. Organização do código

```text
src/main/java/br/edu/acudes/
├── App.java                         composição e ponto de entrada
├── cli/Cli.java                     interpreta comandos diretos
├── cli/MenuTerminal.java             navega pelo menu interativo
├── cli/AcoesTerminal.java            executa ações CRUD e apresenta resultados
├── cli/TerminalUI.java               cores, prompts e carregamento
├── config/Config.java               host, porta e banco via ambiente
├── infra/                           conexão, schema e tipos ReQL
├── model/                           reservatório, leitura e status
├── repository/                      consultas e operações no banco
└── service/                         regras de negócio e backup
```

No modo interativo, o fluxo é `MenuTerminal → AcoesTerminal → Service → Repository → RethinkDB`. `Cli` interpreta comandos diretos, `MenuTerminal` cuida da navegação e `AcoesTerminal` encaminha operações e apresenta resultados. `TerminalUI` concentra as cores e as transições. As consultas ReQL ficam nos repositórios; validações e regras de negócio ficam nos serviços.

## 10. Perguntas para discussão

1. Em que tipo de aplicação um changefeed reduz trabalho em comparação com consultas periódicas?
2. Por que duas réplicas em dois nós não garantem failover automático?
3. Quando o custo de um índice secundário compensa o ganho nas consultas?
4. Qual política de retenção e restauração seria adequada para dados de operação hídrica?

## Referências

- [RethinkDB — Changefeeds](https://rethinkdb.com/docs/changefeeds/)
- [RethinkDB — Sharding e replicação](https://rethinkdb.com/docs/sharding-and-replication/)
- [RethinkDB — Failover](https://rethinkdb.com/docs/failover/)
- [RethinkDB — Backup e restauração](https://rethinkdb.com/docs/backup/)
- [RethinkDB — Índices secundários](https://rethinkdb.com/docs/secondary-indexes/)
- [RethinkDB — Uso de memória e cache](https://rethinkdb.com/docs/memory-usage/)
- [Database of Databases — RethinkDB](https://dbdb.io/db/rethinkdb/)
