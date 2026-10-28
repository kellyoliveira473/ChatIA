package org.example.service;

import org.example.model.Usuario;
import org.example.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Regex: Pelo menos 1 letra maiúscula, 1 caractere especial e comprimento mínimo de 8 caracteres
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$";
    private static final Pattern passwordPattern = Pattern.compile(PASSWORD_PATTERN);

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void cadastrar(Usuario usuario, String confirmacao) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new RuntimeException("Nome é obrigatório");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new RuntimeException("E-mail é obrigatório");
        }
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new RuntimeException("Senha é obrigatória");
        }

        // Validação dos padrões de segurança da senha
        if (!passwordPattern.matcher(usuario.getSenha()).matches()) {
            throw new RuntimeException("A senha deve ter no mínimo 8 caracteres, conter pelo menos 1 letra maiúscula e pelo menos 1 caractere especial.");
        }

        if (!usuario.getSenha().equals(confirmacao)) {
            throw new RuntimeException("As senhas não coincidem");
        }

        // Verifica se o e-mail já está cadastrado no banco
        Usuario usuarioExistente = usuarioRepository.buscarPorEmail(usuario.getEmail());
        if (usuarioExistente != null) {
            throw new RuntimeException("Este e-mail já está cadastrado.");
        }

        // Criptografa a senha com jBCrypt antes de salvar no MongoDB
        String senhaHash = BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt());
        usuario.setSenha(senhaHash);

        usuarioRepository.salvar(usuario);
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-mail é obrigatório");
        }
        if (senha == null || senha.isBlank()) {
            throw new RuntimeException("Senha é obrigatória");
        }

        Usuario usuario = usuarioRepository.buscarPorEmail(email);

        // Valida se o usuário existe e se a senha confere com o hash do banco
        if (usuario == null || !BCrypt.checkpw(senha, usuario.getSenha())) {
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        return usuario;
    }

    public void atualizarNome(Usuario usuario, String novoNome) {
        if (usuario == null || usuario.getEmail() == null) {
            throw new RuntimeException("Usuário inválido");
        }
        if (novoNome == null || novoNome.isBlank()) {
            throw new RuntimeException("O nome não pode ficar vazio");
        }

        String nomeAnterior = usuario.getNome();
        usuario.setNome(novoNome.trim());

        try {
            // Atualiza só o nome (a senha em hash não é tocada)
            usuarioRepository.atualizarNome(usuario.getEmail(), usuario.getNome());
        } catch (RuntimeException e) {
            usuario.setNome(nomeAnterior); // desfaz se o banco falhar
            throw e;
        }
    }
}