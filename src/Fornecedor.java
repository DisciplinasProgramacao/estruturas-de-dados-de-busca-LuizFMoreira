public class Fornecedor {

    private static int ultimoID = 10000; // Contador estático conforme PDF [cite: 32]
    private int documento;
    private String nome;
    private Lista<Produto> produtos; // Estrutura escolhida para armazenar os produtos [cite: 33]

    /**
     * Construtor da classe.
     * @param nome Nome do fornecedor (deve ter pelo menos duas palavras).
     */
    public Fornecedor(String nome) {
        // Validação do nome (pelo menos duas palavras) [cite: 25]
        if (nome == null || nome.trim().split("\\s+").length < 2) {
            throw new IllegalArgumentException("O nome do fornecedor deve conter pelo menos duas palavras.");
        }
        
        this.nome = nome;
        // Gera documento sequencialmente [cite: 26]
        this.documento = ++ultimoID; 
        this.produtos = new Lista<>();
    }

    /**
     * Adiciona um produto à lista de produtos vendidos pelo fornecedor.
     * @param novo Produto a ser adicionado.
     */
    public void adicionarProduto(Produto novo) {
        // Não permite produtos nulos [cite: 28]
        if (novo == null) {
            throw new IllegalArgumentException("Não é possível adicionar um produto nulo.");
        }
        produtos.inserirFinal(novo); // Insere na estrutura escolhida [cite: 27]
    }

    public int getDocumento() {
        return documento;
    }

    public String getNome() {
        return nome;
    }

    /**
     * Retorna o código hash correspondente ao documento do fornecedor. [cite: 30]
     */
    @Override
    public int hashCode() {
        return documento;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Fornecedor other = (Fornecedor) obj;
        return documento == other.documento;
    }

    /**
     * Retorna representação textual com nome, documento e histórico de produtos. [cite: 29]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Fornecedor: ").append(nome).append("\n");
        sb.append("Documento: ").append(documento).append("\n");
        sb.append("Produtos vendidos:\n");
        
        // Supondo que a classe Lista tenha um método toString() ou precise ser percorrida
        // Vou usar o toString da lista que geralmente já formata os itens
        sb.append(produtos.toString()); 
        
        return sb.toString();
    }
}