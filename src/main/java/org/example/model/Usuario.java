package org.example.model;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    @BsonId
    private ObjectId id;
    @NotBlank
    private String nome;
    @NotBlank
    private String email;
    @NotBlank
    @Size(min = 8)
    private String senha;
    @NotBlank
    @Size(min = 8)
    private String confimacao;






}
