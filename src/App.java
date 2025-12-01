import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class App {

    static String nomeArquivoDados;
    static Scanner teclado;
    static int quantosProdutos = 0;

    static AVL<String, Produto> produtosBalanceadosPorNome;
    static AVL<Integer, Produto> produtosBalanceadosPorId;

    static TabelaHash<Produto, Lista<Pedido>> pedidosPorProduto;

    static AVL<Integer, Fornecedor> fornecedoresPorId;
    static TabelaHash<Produto, Lista<Fornecedor>> fornecedoresPorProduto;

    static void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void pausa() {
        System.out.println("Digite enter para continuar...");
        teclado.nextLine();
    }

    static void cabecalho() {
        System.out.println("AEDs II COMÉRCIO DE COISINHAS");
        System.out.println("=============================");
    }

    static <T extends Number> T lerOpcao(String mensagem, Class<T> classe) {
        if (mensagem == null || classe == null) {
            System.out.println("Erro interno: parâmetros inválidos.");
            return null;
        }

        System.out.println(mensagem);
        try {
            String entrada = teclado.nextLine().trim();
            if (entrada.isEmpty()) {
                System.out.println("Erro: entrada não pode ser vazia.");
                return null;
            }

            T valor = classe.getConstructor(String.class).newInstance(entrada);
            return valor;

        } catch (NumberFormatException e) {
            System.out.println("Erro: digite um número válido.");
            return null;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            System.out.println("Erro: valor inválido para o tipo solicitado.");
            return null;
        } catch (Exception e) {
            System.out.println("Erro inesperado ao ler opção: " + e.getMessage());
            return null;
        }
    }

    static int menu() {
        cabecalho();
        System.out.println("1 - Procurar produto, por id");
        System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
        System.out.println("3 - Carregar fornecedores do arquivo");
        System.out.println("4 - Gerar relatório de um fornecedor por documento");
        System.out.println("5 - Gerar relatório de fornecedores de um produto");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");

        try {
            String entrada = teclado.nextLine().trim();
            if (entrada.isEmpty()) {
                System.out.println("Erro: opção não pode ser vazia.");
                return -1;
            }

            int opcao = Integer.parseInt(entrada);
            if (opcao < 0 || opcao > 5) {
                System.out.println("Erro: opção deve ser entre 0 e 5.");
                return -1;
            }
            return opcao;

        } catch (NumberFormatException e) {
            System.out.println("Erro: digite um número válido (0 a 5).");
            return -1;
        } catch (Exception e) {
            System.out.println("Erro inesperado no menu: " + e.getMessage());
            return -1;
        }
    }

    static <K> AVL<K, Produto> lerProdutos(String nomeArquivoDados, Function<Produto, K> extratorDeChave) {
        if (nomeArquivoDados == null || extratorDeChave == null) {
            throw new IllegalArgumentException("Parâmetros não podem ser nulos");
        }

        Scanner arquivo = null;
        try {
            File file = new File(nomeArquivoDados);
            if (!file.exists()) {
                throw new IOException("Arquivo não encontrado: " + nomeArquivoDados);
            }
            if (!file.canRead()) {
                throw new IOException("Sem permissão de leitura no arquivo: " + nomeArquivoDados);
            }

            arquivo = new Scanner(file, Charset.forName("UTF-8"));

            if (!arquivo.hasNextInt()) {
                throw new IOException("Formato inválido: número de produtos não encontrado");
            }

            int numProdutos = arquivo.nextInt();
            arquivo.nextLine();

            if (numProdutos < 0) {
                throw new IOException("Número de produtos inválido: " + numProdutos);
            }

            AVL<K, Produto> produtosCadastrados = new AVL<>();
            int produtosCarregados = 0;
            int produtosComErro = 0;

            for (int i = 0; i < numProdutos; i++) {
                if (!arquivo.hasNextLine()) {
                    System.out.println("AVISO: Arquivo incompleto. Esperados " + numProdutos
                            + " produtos, mas encontrados " + produtosCarregados);
                    break;
                }

                String linha = arquivo.nextLine().trim();
                if (linha.isEmpty()) {
                    continue;
                }

                try {
                    Produto produto = Produto.criarDoTexto(linha);
                    K chave = extratorDeChave.apply(produto);
                    produtosCadastrados.inserir(chave, produto);
                    produtosCarregados++;
                } catch (IllegalArgumentException e) {
                    System.err.println("AVISO: Produto inválido na linha " + (i + 1) + " - " + e.getMessage());
                    produtosComErro++;
                } catch (Exception e) {
                    System.err.println("ERRO: Falha ao processar linha " + (i + 1) + ": " + linha);
                    produtosComErro++;
                }
            }

            quantosProdutos = produtosCarregados;
            System.out.println("Carregados " + produtosCarregados + " de " + numProdutos + " produtos (" + produtosComErro + " com erro).");

            return produtosCadastrados;

        } catch (IOException excecaoArquivo) {
            System.err.println("ERRO CRÍTICO: " + excecaoArquivo.getMessage());
            return new AVL<>();
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO ao ler produtos: " + e.getMessage());
            return new AVL<>();
        } finally {
            if (arquivo != null) {
                try {
                    arquivo.close();
                } catch (Exception e) {
                    System.err.println("AVISO: Erro ao fechar arquivo: " + e.getMessage());
                }
            }
        }
    }

    static <K> AVL<K, Fornecedor> lerFornecedores(String nomeArquivoDados, Function<Fornecedor, K> extratorDeChave) {
        if (nomeArquivoDados == null || extratorDeChave == null) {
            throw new IllegalArgumentException("Parâmetros não podem ser nulos");
        }

        Scanner arquivo = null;
        Random random = new Random(42);

        try {
            File file = new File(nomeArquivoDados);
            if (!file.exists()) {
                System.out.println("Arquivo " + nomeArquivoDados + " não encontrado.");
                return new AVL<>();
            }

            if (!file.canRead()) {
                System.out.println("Sem permissão para ler o arquivo " + nomeArquivoDados);
                return new AVL<>();
            }

            arquivo = new Scanner(file, Charset.forName("UTF-8"));

            if (!arquivo.hasNextInt()) {
                System.out.println("Formato inválido: número de fornecedores não encontrado.");
                return new AVL<>();
            }

            int numFornecedores = arquivo.nextInt();
            arquivo.nextLine();

            if (numFornecedores < 0) {
                System.out.println("Número de fornecedores inválido: " + numFornecedores);
                return new AVL<>();
            }

            if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
                System.out.println("AVISO: Não há produtos carregados. Fornecedores não terão produtos associados.");
            }

            AVL<K, Fornecedor> fornecedoresCadastrados = new AVL<>();
            int fornecedoresCarregados = 0;
            int fornecedoresComErro = 0;

            System.out.println("Carregando " + numFornecedores + " fornecedores...");

            for (int i = 0; i < numFornecedores; i++) {
                if (!arquivo.hasNextLine()) {
                    System.out.println("AVISO: Arquivo incompleto. Esperados " + numFornecedores + " fornecedores.");
                    break;
                }

                String linha = arquivo.nextLine().trim();
                if (linha.isEmpty()) {
                    continue;
                }

                try {
                    String nomeFornecedor = linha;
                    Fornecedor fornecedor = new Fornecedor(nomeFornecedor);

                    if (produtosBalanceadosPorId != null && !produtosBalanceadosPorId.vazia()) {
                        selecionarProdutosAleatorios(fornecedor, random);
                    }

                    K chave = extratorDeChave.apply(fornecedor);
                    fornecedoresCadastrados.inserir(chave, fornecedor);
                    fornecedoresPorId.inserir(fornecedor.getDocumento(), fornecedor);

                    fornecedoresCarregados++;

                    if ((i + 1) % 100 == 0) {
                        System.out.println("Carregados " + (i + 1) + " de " + numFornecedores + " fornecedores...");
                    }

                } catch (IllegalArgumentException e) {
                    System.err.println("Fornecedor inválido na linha " + (i + 2) + " (" + linha + "): " + e.getMessage());
                    fornecedoresComErro++;
                } catch (Exception e) {
                    System.err.println("Erro ao processar fornecedor na linha " + (i + 2) + ": " + e.getMessage());
                    fornecedoresComErro++;
                }
            }

            System.out.println("Fornecedores carregados: " + fornecedoresCarregados +
                    " (com erro: " + fornecedoresComErro + ")");

            return fornecedoresCadastrados;

        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de fornecedores: " + e.getMessage());
            return new AVL<>();
        } finally {
            if (arquivo != null) {
                try {
                    arquivo.close();
                } catch (Exception e) {
                    System.err.println("AVISO: Erro ao fechar arquivo.");
                }
            }
        }
    }

    private static void selecionarProdutosAleatorios(Fornecedor fornecedor, Random random) {
        if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
            return;
        }

        int quantProdutos = random.nextInt(6) + 1;

        int produtosAdicionados = 0;
        int tentativas = 0;
        int maxTentativas = quantProdutos * 10;

        while (produtosAdicionados < quantProdutos && tentativas < maxTentativas) {
            try {
                int idAleatorio = random.nextInt(7750) + 10000;
                Produto produto = produtosBalanceadosPorId.pesquisar(idAleatorio);
                fornecedor.adicionarProduto(produto);
                associarFornecedorProduto(produto, fornecedor);
                produtosAdicionados++;

            } catch (NoSuchElementException e) {

            } catch (IllegalArgumentException e) {

            } catch (Exception e) {

            }

            tentativas++;
        }

        if (produtosAdicionados < quantProdutos) {
            System.out.println("AVISO: Fornecedor " + fornecedor.getNome() +
                    " recebeu apenas " + produtosAdicionados +
                    " de " + quantProdutos + " produtos esperados.");
        }
    }

    private static void associarFornecedorProduto(Produto produto, Fornecedor fornecedor) {
        if (produto == null || fornecedor == null) {
            return;
        }

        try {
            Lista<Fornecedor> fornecedoresDoProduto = fornecedoresPorProduto.pesquisar(produto);
            fornecedoresDoProduto.inserirFinal(fornecedor);

        } catch (NoSuchElementException e) {
            Lista<Fornecedor> fornecedoresDoProduto = new Lista<>();
            fornecedoresDoProduto.inserirFinal(fornecedor);
            fornecedoresPorProduto.inserir(produto, fornecedoresDoProduto);
        }
    }

    static <K> Produto localizarProduto(ABB<K, Produto> produtosCadastrados, K procurado) {
        if (produtosCadastrados == null) {
            System.out.println("Árvore de produtos não inicializada.");
            return null;
        }

        if (procurado == null) {
            System.out.println("Chave de pesquisa não pode ser nula.");
            return null;
        }

        cabecalho();
        System.out.println("Localizando um produto...");

        try {
            Produto produto = produtosCadastrados.pesquisar(procurado);
            System.out.println("Número de comparações realizadas: " + produtosCadastrados.getComparacoes());
            System.out.println("Tempo de processamento da pesquisa: " + produtosCadastrados.getTempo() + " ms");
            return produto;
        } catch (NoSuchElementException excecao) {
            System.out.println("Produto não encontrado.");
            return null;
        } catch (Exception e) {
            System.out.println("Erro na pesquisa: " + e.getMessage());
            return null;
        }
    }

    static Produto localizarProdutoID(ABB<Integer, Produto> produtosCadastrados) {
        Integer idProduto = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        if (idProduto == null) {
            System.out.println("ID do produto inválido.");
            return null;
        }
        return localizarProduto(produtosCadastrados, idProduto);
    }

    static Fornecedor localizarFornecedorPorDocumento(int documento) {
        if (fornecedoresPorId == null) {
            System.out.println("Árvore de fornecedores não inicializada.");
            return null;
        }

        try {
            return fornecedoresPorId.pesquisar(documento);
        } catch (NoSuchElementException e) {
            System.out.println("Fornecedor com documento " + documento + " não encontrado.");
            return null;
        } catch (Exception e) {
            System.out.println("Erro ao pesquisar fornecedor: " + e.getMessage());
            return null;
        }
    }

    private static void mostrarProduto(Produto produto) {
        cabecalho();
        String mensagem = "Produto não encontrado ou dados inválidos.";

        if (produto != null) {
            mensagem = String.format("Dados do produto:\n%s", produto);
        }

        System.out.println(mensagem);
    }

    static void relatorioDeFornecedor() {
        cabecalho();
        System.out.println("=== RELATÓRIO DE FORNECEDOR ===");

        if (fornecedoresPorId == null || fornecedoresPorId.vazia()) {
            System.out.println("Nenhum fornecedor carregado no sistema.");
            System.out.println("Use a opção 3 para carregar fornecedores primeiro.");
            return;
        }

        Integer documento = lerOpcao("Digite o documento identificador do fornecedor: ", Integer.class);
        if (documento == null) {
            System.out.println("Documento inválido.");
            return;
        }

        try {
            Fornecedor fornecedor = fornecedoresPorId.pesquisar(documento);

            System.out.println("\n=== RELATÓRIO COMPLETO DO FORNECEDOR ===");
            System.out.println(fornecedor.toString());

            System.out.println("\n=== ESTATÍSTICAS DE PESQUISA ===");
            System.out.println("Número de comparações realizadas: " + fornecedoresPorId.getComparacoes());
            System.out.println("Tempo de processamento da pesquisa: " + fornecedoresPorId.getTempo() + " ms");

            System.out.print("\nDeseja salvar este relatório em arquivo? (s/n): ");
            String resposta = teclado.nextLine().trim().toLowerCase();

            if (resposta.equals("s") || resposta.equals("sim")) {
                String nomeArquivo = "RelatorioFornecedor_" + documento + ".txt";

                try (FileWriter arquivo = new FileWriter(nomeArquivo, Charset.forName("UTF-8"))) {
                    arquivo.write("=== RELATÓRIO COMPLETO DO FORNECEDOR ===\n");
                    arquivo.write(fornecedor.toString());
                    arquivo.write("\n=== ESTATÍSTICAS DE PESQUISA ===\n");
                    arquivo.write("Número de comparações realizadas: " + fornecedoresPorId.getComparacoes() + "\n");
                    arquivo.write("Tempo de processamento da pesquisa: " + fornecedoresPorId.getTempo() + " ms\n");

                    System.out.println("Relatório salvo em: " + nomeArquivo);
                } catch (IOException e) {
                    System.out.println("Erro ao salvar arquivo: " + e.getMessage());
                }
            }

        } catch (NoSuchElementException e) {
            System.out.println("Fornecedor com documento " + documento + " não encontrado.");
        } catch (Exception e) {
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    static void fornecedoresDoProduto() {
        cabecalho();
        System.out.println("=== FORNECEDORES DE UM PRODUTO ===");

        if (fornecedoresPorProduto == null) {
            System.out.println("Tabela de fornecedores por produto não inicializada.");
            return;
        }

        Produto produto = localizarProdutoID(produtosBalanceadosPorId);
        if (produto == null) {
            System.out.println("Produto não encontrado.");
            return;
        }

        try {
            Lista<Fornecedor> fornecedoresDoProduto = fornecedoresPorProduto.pesquisar(produto);

            System.out.println("\n=== FORNECEDORES DO PRODUTO: " + produto.descricao + " ===");
            System.out.println("ID do produto: " + produto.hashCode());
            System.out.println("Quantidade de fornecedores: " + fornecedoresDoProduto.tamanho());

            String nomeArquivo = "FornecedoresProduto_" + produto.hashCode() + ".txt";

            try (FileWriter arquivo = new FileWriter(nomeArquivo, Charset.forName("UTF-8"))) {
                arquivo.write("=== RELATÓRIO DE FORNECEDORES DO PRODUTO ===\n");
                arquivo.write("Produto: " + produto.descricao + "\n");
                arquivo.write("ID: " + produto.hashCode() + "\n");
                arquivo.write("Data de geração: " + LocalDate.now() + "\n");
                arquivo.write("============================================\n\n");

                arquivo.write("QUANTIDADE DE FORNECEDORES: " + fornecedoresDoProduto.tamanho() + "\n\n");

                if (fornecedoresDoProduto.vazia()) {
                    arquivo.write("Nenhum fornecedor encontrado para este produto.\n");
                } else {
                    arquivo.write("LISTA DE FORNECEDORES:\n");
                    arquivo.write("======================\n");

                    String fornecedoresStr = fornecedoresDoProduto.toString();
                    String[] linhas = fornecedoresStr.split("\n");
                    int contador = 1;

                    for (String linha : linhas) {
                        arquivo.write(contador + ". " + linha + "\n");
                        contador++;
                    }
                }

                arquivo.write("\n=== ESTATÍSTICAS DA PESQUISA ===\n");
                arquivo.write("Número de comparações realizadas: " + fornecedoresPorProduto.getComparacoes() + "\n");
                arquivo.write("Tempo de processamento da pesquisa: " + fornecedoresPorProduto.getTempo() + " ms\n");

                System.out.println("Relatório gerado com sucesso!");
                System.out.println("Arquivo salvo como: " + nomeArquivo);
                System.out.println("Total de fornecedores: " + fornecedoresDoProduto.tamanho());

            } catch (IOException e) {
                System.out.println("Erro ao salvar arquivo: " + e.getMessage());
            }

        } catch (NoSuchElementException e) {
            System.out.println("Nenhum fornecedor encontrado para o produto: " + produto.descricao);

            String nomeArquivo = "FornecedoresProduto_" + produto.hashCode() + ".txt";
            try (FileWriter arquivo = new FileWriter(nomeArquivo, Charset.forName("UTF-8"))) {
                arquivo.write("=== RELATÓRIO DE FORNECEDORES DO PRODUTO ===\n");
                arquivo.write("Produto: " + produto.descricao + "\n");
                arquivo.write("ID: " + produto.hashCode() + "\n");
                arquivo.write("Data: " + LocalDate.now() + "\n");
                arquivo.write("============================================\n\n");
                arquivo.write("NENHUM FORNECEDOR ENCONTRADO PARA ESTE PRODUTO.\n");

                System.out.println("Relatório informativo salvo em: " + nomeArquivo);
            } catch (IOException ex) {
                System.out.println("Erro ao salvar arquivo informativo: " + ex.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private static void gerarPedidos(int quantidade) {
        if (quantidade <= 0) {
            System.out.println("AVISO: Quantidade de pedidos inválida: " + quantidade);
            return;
        }

        if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
            System.out.println("ERRO: Não há produtos carregados para gerar pedidos.");
            return;
        }

        System.out.println("Gerando " + quantidade + " pedidos...");

        Lista<Pedido> pedidos = new Lista<>();
        Random sorteio = new Random(42);
        int pedidosComErro = 0;
        int produtosNaoEncontrados = 0;
        int pedidosVazios = 0;
        int totalProdutosAdicionados = 0;

        for (int i = 0; i < quantidade; i++) {
            try {
                int formaDePagamento = sorteio.nextInt(2) + 1;
                Pedido pedido = new Pedido(LocalDate.now(), formaDePagamento);
                int quantProdutos = sorteio.nextInt(8) + 1;

                for (int j = 0; j < quantProdutos; j++) {
                    try {
                        int id = sorteio.nextInt(7750) + 10_000;
                        Produto produto = produtosBalanceadosPorId.pesquisar(id);
                        pedido.incluirProduto(produto);
                        inserirNaTabela(produto, pedido);
                        totalProdutosAdicionados++;
                    } catch (NoSuchElementException e) {
                        produtosNaoEncontrados++;
                    } catch (Exception e) {

                    }
                }

                if (pedido.getQuantosProdutos() > 0) {
                    pedidos.inserirFinal(pedido);
                } else {
                    pedidosVazios++;
                    pedidosComErro++;
                }

            } catch (Exception e) {
                pedidosComErro++;
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("Gerados " + (i + 1) + " de " + quantidade + " pedidos...");
            }
        }

        System.out.println("Geração de pedidos concluída:");
        System.out.println("- Pedidos gerados com sucesso: " + pedidos.tamanho());
        System.out.println("- Pedidos vazios (sem produtos): " + pedidosVazios);
        System.out.println("- Pedidos com erro: " + pedidosComErro);
        System.out.println("- Produtos não encontrados: " + produtosNaoEncontrados);
        System.out.println("- Total de produtos adicionados: " + totalProdutosAdicionados);
    }

    private static void inserirNaTabela(Produto produto, Pedido pedido) {
        if (produto == null || pedido == null) {
            System.err.println("AVISO: Tentativa de inserir pedido com produto nulo.");
            return;
        }

        try {
            Lista<Pedido> pedidosDoProduto = pedidosPorProduto.pesquisar(produto);
            pedidosDoProduto.inserirFinal(pedido);
        } catch (NoSuchElementException excecao) {
            try {
                Lista<Pedido> pedidosDoProduto = new Lista<>();
                pedidosDoProduto.inserirFinal(pedido);
                pedidosPorProduto.inserir(produto, pedidosDoProduto);
            } catch (Exception e) {
                System.err.println("ERRO ao criar nova lista de pedidos: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("ERRO ao inserir na tabela: " + e.getMessage());
        }
    }

    static void pedidosDoProduto() {
        try {
            Produto produto = localizarProdutoID(produtosBalanceadosPorId);
            if (produto == null) {
                System.out.println("Produto não encontrado.");
                return;
            }

            String nomeArquivo = "RelatorioProduto" + produto.hashCode() + ".txt";

            File arquivo = new File(nomeArquivo);
            if (arquivo.exists() && !arquivo.canWrite()) {
                System.out.println("Não é possível escrever no arquivo: " + nomeArquivo);
                return;
            }

            try (FileWriter arquivoRelatorio = new FileWriter(arquivo, Charset.forName("UTF-8"))) {
                Lista<Pedido> pedidosDoProduto = pedidosPorProduto.pesquisar(produto);
                arquivoRelatorio.append("Relatório de Pedidos - " + produto.descricao + "\n");
                arquivoRelatorio.append("========================================\n");
                arquivoRelatorio.append(pedidosDoProduto.toString() + "\n");
                System.out.println("Dados salvos em " + nomeArquivo);
                System.out.println("Total de pedidos: " + pedidosDoProduto.tamanho());
            } catch (NoSuchElementException e) {
                System.out.println("Nenhum pedido encontrado para este produto.");
            } catch (IOException e) {
                System.out.println("Erro ao salvar arquivo: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            teclado = new Scanner(System.in, Charset.forName("UTF-8"));
            nomeArquivoDados = "produtos.txt";

            System.out.println("Inicializando sistema...");

            produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
            if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
                System.out.println("AVISO: Nenhum produto foi carregado. O sistema pode não funcionar corretamente.");
            } else {
                System.out.println("Produtos carregados: " + quantosProdutos);
            }

            try {
                if (produtosBalanceadosPorId != null && !produtosBalanceadosPorId.vazia()) {
                    produtosBalanceadosPorNome = new AVL<>(produtosBalanceadosPorId,
                            produto -> produto.descricao, String::compareTo);
                } else {
                    produtosBalanceadosPorNome = new AVL<>();
                }
            } catch (Exception e) {
                System.out.println("Erro ao criar árvore por nome: " + e.getMessage());
                produtosBalanceadosPorNome = new AVL<>();
            }

            try {
                int capacidade = Math.max((int) (quantosProdutos * 1.25), 100);
                pedidosPorProduto = new TabelaHash<>(capacidade);

                if (produtosBalanceadosPorId != null && !produtosBalanceadosPorId.vazia()) {
                    gerarPedidos(25000);
                }
            } catch (Exception e) {
                System.out.println("Erro ao inicializar pedidos: " + e.getMessage());
                pedidosPorProduto = new TabelaHash<>(100);
            }

            try {
                fornecedoresPorId = new AVL<>();
                System.out.println("Árvore AVL para fornecedores instanciada.");

                int capacidadeFornecedores = Math.max((int) (quantosProdutos * 1.5), 150);
                fornecedoresPorProduto = new TabelaHash<>(capacidadeFornecedores);
                System.out.println("Tabela hash para fornecedores por produto instanciada.");

            } catch (Exception e) {
                System.out.println("Erro ao inicializar estruturas de fornecedores: " + e.getMessage());
                fornecedoresPorId = new AVL<>();
                fornecedoresPorProduto = new TabelaHash<>(100);
            }

            int opcao = -1;
            do {
                try {
                    opcao = menu();
                    switch (opcao) {
                        case 1 -> {
                            if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
                                System.out.println("Nenhum produto disponível para pesquisa.");
                            } else {
                                mostrarProduto(localizarProdutoID(produtosBalanceadosPorId));
                            }
                        }
                        case 2 -> {
                            if (pedidosPorProduto == null || pedidosPorProduto.tamanho() == 0) {
                                System.out.println("Nenhum pedido disponível para relatório.");
                            } else {
                                pedidosDoProduto();
                            }
                        }
                        case 3 -> {
                            if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
                                System.out.println("Primeiro carregue os produtos para poder carregar fornecedores.");
                            } else {
                                AVL<Integer, Fornecedor> fornecedoresCarregados = lerFornecedores(
                                        "fornecedores.txt",
                                        Fornecedor::getDocumento);

                                if (!fornecedoresCarregados.vazia()) {
                                    System.out.println("Fornecedores carregados com sucesso na árvore AVL!");
                                }
                            }
                        }
                        case 4 -> {
                            relatorioDeFornecedor();
                        }
                        case 5 -> {
                            fornecedoresDoProduto();
                        }
                        case 0 -> System.out.println("Saindo do sistema...");
                        case -1 -> {
                        }
                        default -> System.out.println("Opção inválida! Digite 0 a 5.");
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao processar opção: " + e.getMessage());
                }
                if (opcao != 0) {
                    pausa();
                }
            } while (opcao != 0);

        } catch (Exception e) {
            System.err.println("ERRO FATAL na inicialização: " + e.getMessage());
        } finally {
            if (teclado != null) {
                try {
                    teclado.close();
                } catch (Exception e) {
                    System.err.println("AVISO: Erro ao fechar recursos.");
                }
            }
        }
    }
}
