import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;
import java.io.File;
import java.io.IOException;


public class App {

    /** Nome do arquivo de dados. O arquivo deve estar localizado na raiz do projeto */
    static String nomeArquivoDados;

    /** Scanner para leitura de dados do teclado */
    static Scanner teclado;

    /** Quantidade de produtos cadastrados atualmente na lista */
    static int quantosProdutos = 0;

    /** AGORA TUDO É AVL (conforme a Tarefa 1) */
    static AVL<String, Produto> produtosCadastradosPorNome;
    static AVL<Integer, Produto> produtosCadastradosPorId;

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
        } catch (Exception e) {
            return null;
        }
        return valor;
    }

    /** MENU PRINCIPAL */
    static int menu() {
        cabecalho();
        System.out.println("1 - Carregar produtos por nome/descrição");
        System.out.println("2 - Carregar produtos por id");
        System.out.println("3 - Procurar produto, por nome");
        System.out.println("4 - Procurar produto, por id");
        System.out.println("5 - Remover produto, por nome");
        System.out.println("6 - Remover produto, por id");
        System.out.println("7 - Recortar a lista de produtos, por nome");
        System.out.println("8 - Recortar a lista de produtos, por id");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");
        return Integer.parseInt(teclado.nextLine());
    }

    /**
     * TAREFA 1:
     * - Retorna AVL<K, Produto>
     * - Instancia AVL<K, Produto>
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
            if (arquivo != null)
                arquivo.close();
        }

        return produtosCadastrados; 
    }

    /** LOCALIZA PRODUTO EM AVL */
    static <K> Produto localizarProduto(AVL<K, Produto> produtosCadastrados, K procurado) {

        cabecalho();
        System.out.println("Localizando um produto...");
        Produto produto;

        try {
            produto = produtosCadastrados.pesquisar(procurado);
        } catch (NoSuchElementException e) {
            produto = null;
        }

        System.out.println("Número de comparações realizadas: " + produtosCadastrados.getComparacoes());
        System.out.println("Tempo de processamento da pesquisa: " + produtosCadastrados.getTempo() + " ms");

        return produto;
    }

    static Produto localizarProdutoID(AVL<Integer, Produto> produtosCadastrados) {
        Integer id = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        return localizarProduto(produtosCadastrados, id);
    }

    static Produto localizarProdutoNome(AVL<String, Produto> produtosCadastrados) {
        System.out.println("Digite o nome ou a descrição do produto desejado:");
        String descricao = teclado.nextLine();
        return localizarProduto(produtosCadastrados, descricao);
    }

    private static void mostrarProduto(Produto produto) {
        cabecalho();
        if (produto == null)
            System.out.println("Dados inválidos para o produto!");
        else
            System.out.println("Dados do produto:\n" + produto);
    }

    /** REMOÇÃO EM AVL */
    static <K> Produto removerProduto(AVL<K, Produto> produtosCadastrados, K chave) {
        cabecalho();
        return produtosCadastrados.remover(chave);
    }

    static Produto removerProdutoId(AVL<Integer, Produto> produtosCadastrados) {
        cabecalho();
        Integer id = lerOpcao("Digite o id do produto que deve ser removido:", Integer.class);
        return removerProduto(produtosCadastrados, id);
    }

    private static void recortarProdutosNome(AVL<String, Produto> produtosCadastrados) {

    cabecalho();
    System.out.println("RECORTE DE PRODUTOS POR NOME");
    System.out.println("----------------------------------");

    if (produtosCadastrados == null) {
        System.out.println("A árvore ainda não foi carregada! Use a opção 1 primeiro.");
        return;
    }

    // Leitura das chaves do intervalo
    System.out.print("Digite o nome INICIAL do intervalo: ");
    String chaveInicio = teclado.nextLine();

    System.out.print("Digite o nome FINAL do intervalo: ");
    String chaveFim = teclado.nextLine();

    // Realiza o recorte usando o método da AVL
    Lista<Produto> resultado = produtosCadastrados.recortar(chaveInicio, chaveFim);

    cabecalho();
    System.out.println("RESULTADO DO RECORTE:");
    System.out.println("----------------------");

    if (resultado == null || resultado.vazia()) {
        System.out.println("Nenhum produto encontrado no intervalo informado!");
        return;
    }

    System.out.println(resultado.toString());
}



    private static void recortarProdutosId(AVL<Integer, Produto> produtosCadastrados) {

    cabecalho();
    System.out.println("RECORTE DE PRODUTOS POR ID");
    System.out.println("----------------------------------");

    if (produtosCadastrados == null) {
        System.out.println("A árvore ainda não foi carregada! Use a opção 2 primeiro.");
        return;
    }

    // Lê chaves numéricas
    System.out.print("Digite o ID INICIAL do intervalo: ");
    Integer chaveInicio = Integer.parseInt(teclado.nextLine());

    System.out.print("Digite o ID FINAL do intervalo: ");
    Integer chaveFim = Integer.parseInt(teclado.nextLine());

    // Recorte na própria AVL
    Lista<Produto> resultado = produtosCadastrados.recortar(chaveInicio, chaveFim);

    cabecalho();
    System.out.println("RESULTADO DO RECORTE:");
    System.out.println("----------------------");

    if (resultado == null || resultado.vazia()) {
        System.out.println("Nenhum produto encontrado no intervalo informado!");
        return;
    }

    System.out.println(resultado.toString());
}
static Produto removerProdutoNome(AVL<String, Produto> produtosCadastrados) {
    cabecalho();
    System.out.println("Digite a descrição do produto que deve ser removido:");
    String nome = teclado.nextLine();
    return removerProduto(produtosCadastrados, nome);
}




    public static void main(String[] args) {

        teclado = new Scanner(System.in, Charset.forName("UTF-8"));
        nomeArquivoDados = "produtos.txt";

        int opcao;

        do {
            opcao = menu();
            switch (opcao) {

                case 1 -> produtosCadastradosPorNome =
                        lerProdutos(nomeArquivoDados, p -> p.descricao);

                case 2 -> produtosCadastradosPorId =
                        lerProdutos(nomeArquivoDados, p -> p.idProduto);

                case 3 -> mostrarProduto(localizarProdutoNome(produtosCadastradosPorNome));

                case 4 -> mostrarProduto(localizarProdutoID(produtosCadastradosPorId));

                case 5 -> mostrarProduto(removerProdutoNome(produtosCadastradosPorNome));

                case 6 -> mostrarProduto(removerProdutoId(produtosCadastradosPorId));

                case 7 -> recortarProdutosNome(produtosCadastradosPorNome);

                case 8 -> recortarProdutosId(produtosCadastradosPorId);
            }

            pausa();
        } while (opcao != 0);

        teclado.close();
    }
}
