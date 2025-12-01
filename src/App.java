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

    /** Nome do arquivo de dados. O arquivo deve estar localizado na raiz do projeto */
    static String nomeArquivoDados;
    
    /** Scanner para leitura de dados do teclado */
    static Scanner teclado;

    /** Quantidade de produtos cadastrados atualmente na lista */
    static int quantosProdutos = 0;

    static AVL<String, Produto> produtosBalanceadosPorNome;
    
    static AVL<Integer, Produto> produtosBalanceadosPorId;
    
    static TabelaHash<Produto, Lista<Pedido>> pedidosPorProduto;
    
    static void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Gera um efeito de pausa na CLI. Espera por um enter para continuar */
    static void pausa() {
        System.out.println("Digite enter para continuar...");
        teclado.nextLine();
    }

    /** Cabeçalho principal da CLI do sistema */
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
    
    /** Imprime o menu principal, lê a opção do usuário e a retorna (int). */
    static int menu() {
        cabecalho();
        System.out.println("1 - Procurar produto, por id");
        System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");
        
        try {
            String entrada = teclado.nextLine().trim();
            if (entrada.isEmpty()) {
                System.out.println("Erro: opção não pode ser vazia.");
                return -1;
            }
            
            int opcao = Integer.parseInt(entrada);
            if (opcao < 0 || opcao > 2) {
                System.out.println("Erro: opção deve ser 0, 1 ou 2.");
                return -1;
            }
            return opcao;
            
        } catch (NumberFormatException e) {
            System.out.println("Erro: digite um número válido (0, 1 ou 2).");
            return -1;
        } catch (Exception e) {
            System.out.println("Erro inesperado no menu: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Lê os dados de um arquivo-texto e retorna uma árvore de produtos.
     */
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
            arquivo.nextLine(); // Consumir quebra de linha
            
            if (numProdutos < 0) {
                throw new IOException("Número de produtos inválido: " + numProdutos);
            }
            
            AVL<K, Produto> produtosCadastrados = new AVL<>();
            int produtosCarregados = 0;
            int produtosComErro = 0;
            
            for (int i = 0; i < numProdutos; i++) {
                if (!arquivo.hasNextLine()) {
                    System.out.println("AVISO: Arquivo incompleto. Esperados " + numProdutos + " produtos, mas encontrados " + produtosCarregados);
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
    
    /** Localiza um produto na árvore de produtos organizados por id */
    static Produto localizarProdutoID(ABB<Integer, Produto> produtosCadastrados) {
        Integer idProduto = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        if (idProduto == null) {
            System.out.println("ID do produto inválido.");
            return null;
        }
        return localizarProduto(produtosCadastrados, idProduto);
    }
    
    private static void mostrarProduto(Produto produto) {
        cabecalho();
        String mensagem = "Produto não encontrado ou dados inválidos.";
        
        if (produto != null){
            mensagem = String.format("Dados do produto:\n%s", produto);
        }
        
        System.out.println(mensagem);
    }
    
    /** MÉTODO CRÍTICO - CORRIGIDO COM TRATAMENTO COMPLETO */
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
                    // Continua com o próximo produto
                } catch (Exception e) {
                    System.err.println("AVISO: Erro ao adicionar produto ao pedido: " + e.getMessage());
                    // Continua com o próximo produto
                }
            }
            
            // Usa getQuantosProdutos() que existe na classe Pedido
            if (pedido.getQuantosProdutos() > 0) {
                pedidos.inserirFinal(pedido);
            } else {
                pedidosVazios++;
                pedidosComErro++;
            }
            
        } catch (Exception e) {
            System.err.println("ERRO ao gerar pedido " + i + ": " + e.getMessage());
            pedidosComErro++;
        }
        
        // Progresso a cada 1000 pedidos
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
            
            // Verifica se pode escrever no arquivo
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
            
            // Carregar produtos com tratamento de erro
            produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
            if (produtosBalanceadosPorId == null || produtosBalanceadosPorId.vazia()) {
                System.out.println("AVISO: Nenhum produto foi carregado. O sistema pode não funcionar corretamente.");
            } else {
                System.out.println("Produtos carregados: " + quantosProdutos);
            }
            
            // Criar árvore por nome com tratamento de erro
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
            
            // Inicializar tabela hash
            try {
                int capacidade = Math.max((int)(quantosProdutos * 1.25), 100);
                pedidosPorProduto = new TabelaHash<>(capacidade);
                
                if (produtosBalanceadosPorId != null && !produtosBalanceadosPorId.vazia()) {
                    gerarPedidos(25000);
                }
            } catch (Exception e) {
                System.out.println("Erro ao inicializar pedidos: " + e.getMessage());
                pedidosPorProduto = new TabelaHash<>(100);
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
                        case 0 -> System.out.println("Saindo do sistema...");
                        case -1 -> {} // Opção inválida já tratada no menu()
                        default -> System.out.println("Opção inválida! Digite 0, 1 ou 2.");
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao processar opção: " + e.getMessage());
                }
                if (opcao != 0) {
                    pausa();
                }
            } while(opcao != 0);       

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
