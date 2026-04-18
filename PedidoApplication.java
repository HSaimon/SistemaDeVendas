package com.example.SistemaDeVendas.applications;

import com.example.SistemaDeVendas.configs.RegraNegocioException;
import com.example.SistemaDeVendas.entities.*;
import com.example.SistemaDeVendas.interfacies.IPedido;
import com.example.SistemaDeVendas.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PedidoApplication implements IPedido {

    private final PedidoRepositoryMySql pedidoRepository;
    private final ProdutoRepositoryMySql produtoRepository;
    private final DescontoFidelidadeRepositoryMySql descontoFidelidadeRepository;
    private final ClienteRepositoryMySql clienteRepository;

    @Autowired
    public PedidoApplication(PedidoRepositoryMySql pedidoRepository,
                              ProdutoRepositoryMySql produtoRepository,
                              ClienteRepositoryMySql clienteRepository,
                              DescontoFidelidadeRepositoryMySql descontoFidelidadeRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.descontoFidelidadeRepository = descontoFidelidadeRepository;
    }

    @Override
    public Pedido buscarPorId(int id) {
        return this.pedidoRepository.buscarPorId(id);
    }

    @Override
    public List<Pedido> buscarTodos() {
        return this.pedidoRepository.buscarTodos();
    }

    @Override
    public void salvar(Pedido pedido) {
        if (pedido.getItemPedidos() == null) {
            pedido.setItemPedidos(new ArrayList<>());
        }

        if (pedido.getItemPedidos().isEmpty()) {
            throw new RegraNegocioException("O pedido deve conter pelo menos um item.");
        }

        // 1. Valida e baixa estoque de cada item
        for (ItemPedido item : pedido.getItemPedidos()) {
            if (item.getIdProduto() == null) {
                throw new RegraNegocioException("Item do pedido sem produto associado.");
            }

            Produto produto = produtoRepository.buscarPorId(item.getIdProduto().getId());
            if (produto == null) {
                throw new RegraNegocioException(
                        "Produto com ID " + item.getIdProduto().getId() + " não encontrado.");
            }

            // verificarEstoque() retorna true quando estoque < quantidade (insuficiente)
            if (produto.verificarEstoque(item.getQuantidade())) {
                throw new RegraNegocioException(
                        "Estoque insuficiente para o produto '" + produto.getNome() +
                        "'. Disponível: " + produto.getEstoque() +
                        ", Solicitado: " + item.getQuantidade());
            }

            produto.baixarEstoque(item.getQuantidade());
            produtoRepository.atualizar(produto.getId(), produto);

            // Alerta de estoque mínimo (não bloqueia a venda)
            if (produto.getEstoque() <= produto.getMinEstoque()) {
                System.out.printf(
                    "[ALERTA-ESTOQUE] Produto '%s' (ID: %d) atingiu o estoque mínimo! " +
                    "Atual: %d, Mínimo: %d%n",
                    produto.getNome(), produto.getId(),
                    produto.getEstoque(), produto.getMinEstoque()
                );
            }

            // Garante preço unitário atualizado e vínculo com o pedido
            item.setPrecoUnitario(produto.getPreco());
            item.setIdPedido(pedido);
        }

        // 2. Calcula valor total com base nos itens
        pedido.calcularValorTotal();

        // 3. Aplica desconto de fidelidade — opcional (CORRIGIDO: verificação de nulidade)
        if (pedido.getDescontoFidelidade() != null && pedido.getDescontoFidelidade().getId() != 0) {
            DescontoFidelidade desconto = descontoFidelidadeRepository
                    .buscarPorId(pedido.getDescontoFidelidade().getId());

            if (desconto == null) {
                throw new RegraNegocioException("Desconto de fidelidade não encontrado.");
            }
            if (desconto.verificarVencimento()) {
                throw new RegraNegocioException("O desconto de fidelidade está expirado.");
            }
            if (desconto.verificarValorDesconto(pedido.getValorTotal())) {
                throw new RegraNegocioException(
                        "O valor do desconto não pode ser maior que o valor total do pedido.");
            }

            float valorDesconto = desconto.valorDesconto(pedido.getValorTotal());
            pedido.aplicarDesconto(valorDesconto);
            descontoFidelidadeRepository.atualizar(desconto.getId(), desconto);
        }

        // 4. Define status inicial e data do pedido
        pedido.atualizarStatusPagamento();
        if (pedido.getDataPedido() == null) {
            pedido.setDataPedido(LocalDate.now());
        }

        // 5. CORRIGIDO: salvar vem ANTES de qualquer atualizar; atualizar redundante removido
        this.pedidoRepository.salvar(pedido);

        // 6. Atualiza categoria de fidelidade do cliente após a venda
        if (pedido.getIdCliente() != null) {
            Cliente cliente = clienteRepository.buscarPorId(pedido.getIdCliente().getId());
            if (cliente != null) {
                cliente.atualizarCategoria();
                clienteRepository.atualizar(cliente.getId(), cliente);
            }
        }
    }

    @Override
    public void atualizar(int id, Pedido pedido) {
        Pedido pedidoInDB = this.pedidoRepository.buscarPorId(id);
        if (pedidoInDB == null) {
            throw new RegraNegocioException("Pedido com ID " + id + " não encontrado.");
        }
        this.pedidoRepository.atualizar(id, pedido);
    }

    @Override
    public void deletar(int id) {
        Pedido pedido = this.pedidoRepository.buscarPorId(id);
        if (pedido == null) {
            throw new RegraNegocioException("Pedido com ID " + id + " não encontrado.");
        }
        this.pedidoRepository.deletar(id);
    }
}
