🚀 Luna IA — Assistente Virtual Inteligente (Desktop)

O Luna IA (também conhecido como ChatAI) é uma aplicação desktop desenvolvida 
em Java para fornecer uma experiência fluida de interação com Inteligência Artificial, 
persistência de dados em nuvem e uma interface moderna e intuitiva.

🛠️ Tecnologias e Ferramentas Utilizadas

Linguagem: Java 21

Interface Gráfica (GUI): JavaFX

Gerenciador de Dependências: Apache Maven

Banco de Dados (Nuvem): MongoDB Atlas

Provedor de IA: Groq API (modelos LLM de alta velocidade)

Empacotamento & Portabilidade: Launch4j (geração de executável nativo .exe) & Maven Shade Plugin (Uber JAR)

Controle de Versão: Git & GitHub

📌 Principais Funcionalidades

Interação em Tempo Real: Conversas fluidas com o modelo de IA integrado via API da Groq.

Histórico e Persistência: Registro de interações e dados diretamente na nuvem com o MongoDB Atlas.

Portabilidade Total: Executável nativo .exe pronto para rodar
em ambientes Windows sem necessidade de instalações complexas ou configurações de ambiente.

Interface Limpa e Responsiva: Desenvolvida com componentes JavaFX para melhor usabilidade no dia a dia.

📂 Estrutura do Projeto

ChatIA-novo/

├── src/                    # Código-fonte principal da aplicação

│   ├── main/

│   │   ├── java/           # Classes Controller, Model e serviços da IA e MongoDB

│   │   └── resources/      # Arquivos de visualização (FXML), estilos (CSS) e ícones

├── target/                 # Artefatos gerados pelo build do Maven (.jar)

├── dist/                   # Distribuível compilado para ambiente executável

├── pom.xml                 # Configurações de dependências e plugins Maven

└── LunaIA.exe              # Executável portátil da aplicação


🚀 Como Executar o Projeto

A aplicação foi desenvolvida para ser totalmente portátil. 
Não é necessário instalar nenhuma dependência ou ambiente de desenvolvimento (IDE) para utilizá-la.

Execução Simples (Para Usuários Final / Faculdade)

Baixe o arquivo .zip ou o executável LunaIA.exe disponível neste repositório.

Se baixou em formato ZIP, extraia o conteúdo em uma pasta de sua preferência.

Dê dois cliques sobre o arquivo LunaIA.exe.

O aplicativo abrirá imediatamente e estará pronto para uso.

👥 Autoria e Créditos

Este projeto foi desenvolvido exclusivamente por Gleice Kelly Oliveira Barros
como parte do meu desenvolvimento acadêmico e prático, integração de APIs e banco de dados.
