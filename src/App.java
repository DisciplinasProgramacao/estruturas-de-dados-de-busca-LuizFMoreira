import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.io.BufferedWriter;
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
        
    	T valor;
        
    	System.out.println(mensagem);
    	try {
            valor = classe.getConstructor(String.class).newInstance(teclado.nextLine());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException 
        		| InvocationTargetException | NoSuchMethodException | SecurityException e) {
            return null;
        }
        return valor;
    }
    
    /** Imprime o menu principal, lê a opção do usuário e a retorna (int).
     * Perceba que poderia haver uma melhor modularização com a criação de uma classe Menu.
     * @return Um inteiro com a opção do usuário.
    */
    static int menu() {
        cabecalho();
        System.out.println("1 - Procurar produto, por id");
        System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");
        return Integer.parseInt(teclado.nextLine());
    }
    
    /**
     * Lê os dados de um arquivo-texto e retorna uma árvore de produtos. Arquivo-texto no formato
     * N (quantidade de produtos) <br/>
     * tipo;descrição;preçoDeCusto;margemDeLucro;[dataDeValidade] <br/>
     * Deve haver uma linha para cada um dos produtos. Retorna uma árvore vazia em caso de problemas com o arquivo.
     * @param nomeArquivoDados Nome do arquivo de dados a ser aberto.
     * @return Uma árvore com os produtos carregados, ou vazia em caso de problemas de leitura.
     */
    static <K> AVL<K, Produto> lerProdutos(String nomeArquivoDados, Function<Produto, K> extratorDeChave) {
    	
    	Scanner arquivo = null;
    	int numProdutos;
    	String linha;
    	Produto produto;
    	AVL<K, Produto> produtosCadastrados;
    	K chave;
    	
    	try {
    		arquivo = new Scanner(new File(nomeArquivoDados), Charset.forName("UTF-8"));
    		
    		numProdutos = Integer.parseInt(arquivo.nextLine());
    		produtosCadastrados = new AVL<K, Produto>();
    		
    		for (int i = 0; i < numProdutos; i++) {
    			linha = arquivo.nextLine();
    			produto = Produto.criarDoTexto(linha);
    			chave = extratorDeChave.apply(produto);
    			produtosCadastrados.inserir(chave, produto);
    		}
    		quantosProdutos = numProdutos;
    		
    	} catch (IOException excecaoArquivo) {
    		produtosCadastrados = null;
    	} finally {
    		arquivo.close();
    	}
    	
    	return produtosCadastrados;
    }
    
    static <K> Produto localizarProduto(ABB<K, Produto> produtosCadastrados, K procurado) {
    	
    	Produto produto;
    	
    	cabecalho();
    	System.out.println("Localizando um produto...");
    	
    	try {
    		produto = produtosCadastrados.pesquisar(procurado);
    	} catch (NoSuchElementException excecao) {
    		produto = null;
    	}
    	
    	System.out.println("Número de comparações realizadas: " + produtosCadastrados.getComparacoes());
    	System.out.println("Tempo de processamento da pesquisa: " + produtosCadastrados.getTempo() + " ms");
        
    	return produto;
    	
    }
    
    /** Localiza um produto na árvore de produtos organizados por id, a partir do código de produto informado pelo usuário, e o retorna. 
     *  Em caso de não encontrar o produto, retorna null */
    static Produto localizarProdutoID(ABB<Integer, Produto> produtosCadastrados) {
        
        int idProduto = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        
        return localizarProduto(produtosCadastrados, idProduto);
    }
    
    /** Localiza um produto na árvore de produtos organizados por nome, a partir do nome de produto informado pelo usuário, e o retorna. 
     *  A busca não é sensível ao caso. Em caso de não encontrar o produto, retorna null */
    static Produto localizarProdutoNome(ABB<String, Produto> produtosCadastrados) {
        
    	String descricao;
    	
    	System.out.println("Digite o nome ou a descrição do produto desejado:");
        descricao = teclado.nextLine();
        
        return localizarProduto(produtosCadastrados, descricao);
    }
    
    private static void mostrarProduto(Produto produto) {
    	
        cabecalho();
        String mensagem = "Dados inválidos para o produto!";
        
        if (produto != null){
            mensagem = String.format("Dados do produto:\n%s", produto);
        }
        
        System.out.println(mensagem);
    }
    
    private static Lista<Pedido> gerarPedidos(int quantidade) {
        Lista<Pedido> pedidos = new Lista<>();
        Random sorteio = new Random(42);
        int quantProdutos;
        int formaDePagamento;
        for (int i = 0; i < quantidade; i++) {
        	formaDePagamento = sorteio.nextInt(2) + 1;
            Pedido pedido = new Pedido(LocalDate.now(), formaDePagamento);
            quantProdutos = sorteio.nextInt(8) + 1;
            for (int j = 0; j < quantProdutos; j++) {
                int id = sorteio.nextInt(7750) + 10_000;
                Produto produto = produtosBalanceadosPorId.pesquisar(id);
                pedido.incluirProduto(produto);
                inserirNaTabela(produto, pedido);
            }
            pedidos.inserirFinal(pedido);
        }
        return pedidos;
    }
    
   private static void inserirNaTabela(Produto produto, Pedido pedido) {
    // Obtém a lista de pedidos associada ao produto
    Lista<Pedido> lista = pedidosPorProduto.pesquisar(produto);

    // Se ainda não existe lista para esse produto, criar
    if (lista == null) {
        lista = new Lista<>();
        pedidosPorProduto.inserir(produto, lista);
    }

    // Inserir o pedido na lista
    lista.inserirFinal(pedido);
}

    
    
static void pedidosDoProduto() {
    Produto produto = localizarProdutoID(produtosBalanceadosPorId);
    if (produto == null) {
        System.out.println("Produto não encontrado.");
        return;
    }

    Lista<Pedido> pedidosDoProduto = null;
    try {
        pedidosDoProduto = pedidosPorProduto.pesquisar(produto);
    } catch (Exception e) {
        // dependendo da implementação de TabelaHash, pesquisar pode lançar exceção
        pedidosDoProduto = null;
    }

    if (pedidosDoProduto == null) {
        System.out.println("Esse produto não possui pedidos.");
        return;
    }

    // tenta descobrir um nome legível para o arquivo
    String nomeBase = produto.toString().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    String nomeArquivo = "RelatorioProduto_" + nomeBase + ".txt";

    // Gravação em UTF-8 usando Files.newBufferedWriter
    try (BufferedWriter bw = Files.newBufferedWriter(
            Path.of(nomeArquivo),
            Charset.forName("UTF-8"),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {

        bw.write("RELATÓRIO DE PEDIDOS DO PRODUTO");
        bw.newLine();
        bw.write("Produto: " + produto.toString());
        bw.newLine();
        bw.write("Total de pedidos registrados nesta tabela: ");
        // se Lista tiver getTamanho()
        try {
            bw.write(String.valueOf(pedidosDoProduto.getTamanho()));
        } catch (Throwable t) {
            // se não existir getTamanho(), apenas não escreve o total
        }
        bw.newLine();
        bw.newLine();

        // percorre a lista de pedidos: usa getPrimeiro() / getProximo() / getItem()
        // esses nomes são comuns em implementações de lista de AEDs
        try {
            Celula<Pedido> atual = pedidosDoProduto.getPrimeiro();
            int contador = 0;
            while (atual != null) {
                contador++;
                Pedido p = atual.getItem();
                bw.write("Pedido #" + contador);
                bw.newLine();
                // usa toString() do pedido para garantir compatibilidade
                bw.write(p.toString());
                bw.newLine();
                bw.write("--------------------------------------------------");
                bw.newLine();
                atual = atual.getProximo();
            }
        } catch (NoSuchMethodError | NoSuchElementException | NullPointerException e) {
            // Caso a Lista tenha outra API (por exemplo pegar por índice), tenta alternativa:
            try {
                int tamanho = pedidosDoProduto.getTamanho();
                for (int i = 0; i < tamanho; i++) {
                    Pedido p = pedidosDoProduto.pegar(i); // se existir pegar(index)
                    bw.write("Pedido #" + (i+1));
                    bw.newLine();
                    bw.write(p.toString());
                    bw.newLine();
                    bw.write("--------------------------------------------------");
                    bw.newLine();
                }
            } catch (Throwable t) {
                // fallback: escreve mensagem indicando que não foi possível iterar
                bw.write("Não foi possível iterar os pedidos: " + t.getMessage());
                bw.newLine();
            }
        }

        System.out.println("Relatório gerado com sucesso em: " + nomeArquivo);

    } catch (IOException e) {
        System.out.println("Erro ao criar/grav ar o arquivo: " + e.getMessage());
    }
}
    
	public static void main(String[] args) {
		teclado = new Scanner(System.in, Charset.forName("UTF-8"));
        nomeArquivoDados = "produtos.txt";
        produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
        produtosBalanceadosPorNome = new AVL<>(produtosBalanceadosPorId, produto -> produto.descricao, String::compareTo);
        pedidosPorProduto = new TabelaHash<>((int)(quantosProdutos * 1.25));
        
        gerarPedidos(25_000);
       
        int opcao = -1;
      
        do {
            opcao = menu();
            switch (opcao) {
            	case 1 -> mostrarProduto(localizarProdutoID(produtosBalanceadosPorId));
            	case 2 -> pedidosDoProduto(); 
            }
            pausa();
        } while(opcao != 0);       

        teclado.close();    
    }
}