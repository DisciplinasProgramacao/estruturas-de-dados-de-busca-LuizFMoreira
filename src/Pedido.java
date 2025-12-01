import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

public class Pedido implements Comparable<Pedido>{

	private static int ultimoID = 1;
	
	private int idPedido;
	
	/** Porcentagem de desconto para pagamentos à vista */
	private static final double DESCONTO_PG_A_VISTA = 0.15;
	
	/** Lista de produtos do pedido */
	private Lista<Produto> produtos;
	
	/** Data de criação do pedido */
	private LocalDate dataPedido;
	
	/** Indica a quantidade total de produtos no pedido até o momento */
	private int quantProdutos = 0;
	
	/** Indica a forma de pagamento do pedido sendo: 1, pagamento à vista; 2, pagamento parcelado */
	private int formaDePagamento;
	
	/** Construtor do pedido.
	 *  Deve criar a lista de produtos do pedido, 
	 *  armazenar a data, o código identificador e a forma de pagamento informados para o pedido. 
	 */  
	public Pedido(LocalDate dataPedido, int formaDePagamento) {
		
		idPedido = ultimoID++;
		produtos = new Lista<Produto>();
		quantProdutos = 0;
		this.dataPedido = dataPedido;
		this.formaDePagamento = formaDePagamento;
	}
	
	/**
     * Inclui um produto neste pedido e aumenta a quantidade de produtos armazenados no pedido até o momento.
     * @param novo O produto a ser incluído no pedido
     * @return true/false indicando se a inclusão do produto no pedido foi realizada com sucesso.
     */
	public boolean incluirProduto(Produto novo) {
		
		if (novo == null) {
			return false;
		}
		produtos.inserir(novo, quantProdutos);
		quantProdutos++;
		return true;
	}
	
	/**
     * Calcula e retorna o valor final do pedido (soma do valor de venda de todos os produtos do pedido).
     * Caso a forma de pagamento do pedido seja à vista, aplica o desconto correspondente.
     * @return Valor final do pedido (double)
     */
	public double valorFinal() {
		
		double valorPedido = produtos.calcularValorTotal((produto -> produto.valorDeVenda()));
		BigDecimal valorPedidoBD;
		
		if (formaDePagamento == 1) {
			valorPedido = valorPedido * (1.0 - DESCONTO_PG_A_VISTA);
		}
		
		valorPedidoBD = new BigDecimal(Double.toString(valorPedido));
        
		valorPedidoBD = valorPedidoBD.setScale(2, RoundingMode.HALF_UP);
        
        return valorPedidoBD.doubleValue();
	}
	
	/**
     * Representação, em String, do pedido.
     * Contém um cabeçalho com seu código identificador, sua data e a quantidade de produtos no pedido.
     * Depois, em cada linha, a descrição de cada produto do pedido.
     * Ao final, mostra a forma de pagamento, o percentual de desconto (se for o caso) e o valor a ser pago pelo pedido.
     * Exemplo:
     * Número do pedido: 01
     * Data do pedido: 25/08/2025
     * Pedido com 2 produtos.
     * Produtos no pedido:
     * NOME: Iogurte: R$ 8.00
     * Válido até: 29/08/2025
     * NOME: Guardanapos: R$ 2.75
     * Pedido pago à vista. Percentual de desconto: 15,00%
     * Valor total do pedido: R$ 10.75 
     * @return Uma string contendo dados do pedido conforme especificado (cabeçalho, detalhes, forma de pagamento,
     * percentual de desconto - se for o caso - e valor a pagar)
     */
	@Override
	public String toString() {
		
		StringBuilder stringPedido = new StringBuilder();
		
		stringPedido.append(String.format("Número do pedido: %02d\n", idPedido));
		
		DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		stringPedido.append("Data do pedido: " + formatoData.format(dataPedido) + "\n");
		
		stringPedido.append("Pedido com " + quantProdutos + " produtos.\n");
		stringPedido.append("Produtos no pedido:\n");
		stringPedido.append(produtos.toString());
		
		stringPedido.append("Pedido pago ");
		if (formaDePagamento == 1) {
			stringPedido.append("à vista. Percentual de desconto: " + String.format("%.2f", DESCONTO_PG_A_VISTA * 100) + "%\n");
		} else {
			stringPedido.append("parcelado.\n");
		}
		
		stringPedido.append("Valor total do pedido: R$ " + String.format("%.2f", valorFinal()) + "\n");
		
		return stringPedido.toString();
	}
	
    /**
     * Comparação padrão do pedido: identificador.
     * Retorna um valor negativo se este pedido tiver um identificador anterior ao do outro pedido,
     * valor positivo se o identificador for posterior ao do outro pedido. Para o mesmo pedido, o
     * retorno é 0.
     * @param outro Pedido a ser comparado
     * @return Int de acordo com a regra padrão de Comparable (descrita acima)
     */
    @Override
    public int compareTo(Pedido outro) {
    	if (this.idPedido == outro.idPedido) {
    		return 0;
    	} else if (this.idPedido < outro.idPedido) {
    		return -1;
    	} else {
    		return 1;
    	}
    }
    
    public LocalDate getDataPedido() {
    	return dataPedido;
    }
    
    public int getIdPedido() {
    	return idPedido;
    }
    
    public int getQuantosProdutos() {
    	return quantProdutos;
    }
    
    public Lista<Produto> getProdutos() {
    	return produtos;
    }
    
	/**
	 * Conta as repetições de um produto dentro do pedido.  
	 * @param produto Objeto "Produto" com a descrição do produto a ser contado
	 * @return Quantidade de repetições deste produto no pedido
	 */
	public int repeticoes(Produto produto){
		
		Predicate<Produto> condicional = (prod -> prod.descricao.equals(produto.descricao));
		return produtos.contarRepeticoes(condicional);
	}
}