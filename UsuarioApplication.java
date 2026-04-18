package com.example.SistemaDeVendas.applications;

import com.example.SistemaDeVendas.configs.RegraNegocioException;
import com.example.SistemaDeVendas.entities.Usuario;
import com.example.SistemaDeVendas.interfacies.IUsuario;
import com.example.SistemaDeVendas.repositories.UsuarioRepositoryMySql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioApplication implements IUsuario {

    private final UsuarioRepositoryMySql usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioApplication(UsuarioRepositoryMySql usuarioRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    @Override
    public Usuario buscarPorId(int id) {
        return this.usuarioRepository.buscarPorId(id);
    }

    @Override
    public List<Usuario> buscarTodos() {
        return this.usuarioRepository.buscarTodos();
    }

    @Override
    public void salvar(Usuario usuario) {
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new RegraNegocioException("A senha do usuário não pode ser vazia.");
        }
        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            throw new RegraNegocioException("O CPF do usuário não pode ser vazio.");
        }
        // CORRIGIDO: senha codificada com BCrypt antes de persistir
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        this.usuarioRepository.salvar(usuario);
    }

    @Override
    public void atualizar(int id, Usuario usuario) {
        Usuario usuarioInDB = this.usuarioRepository.buscarPorId(id);
        if (usuarioInDB == null) {
            throw new RegraNegocioException("Usuário com ID " + id + " não encontrado.");
        }
        // Só recodifica se uma nova senha foi fornecida
        if (usuario.getSenha() != null && !usuario.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        } else {
            // Mantém a senha atual para não sobrescrever com valor vazio
            usuario.setSenha(usuarioInDB.getSenha());
        }
        this.usuarioRepository.atualizar(id, usuario);
    }

    @Override
    public void deletar(int id) {
        Usuario usuarioInDB = this.usuarioRepository.buscarPorId(id);
        if (usuarioInDB == null) {
            throw new RegraNegocioException("Usuário com ID " + id + " não encontrado.");
        }
        this.usuarioRepository.deletar(id);
    }
}
