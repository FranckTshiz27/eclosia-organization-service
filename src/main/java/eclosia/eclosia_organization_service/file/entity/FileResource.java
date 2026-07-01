package eclosia.eclosia_organization_service.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "file_resources")
public class FileResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Nom physique du fichier enregistre sur le serveur.
     * Exemple :
     * 3f8cb64c-4f62-49d4-b4a6-2d3df1b1f1b8.jpg
     */
    @Column(name = "file_name", nullable = false, unique = true, length = 255)
    private String fileName;

    /**
     * Nom d'origine envoye par l'utilisateur.
     * Exemple :
     * photo-jean.jpg
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * Type MIME.
     * Exemple :
     * image/jpeg
     * image/png
     * application/pdf
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * Taille en octets.
     */
    @Column(nullable = false)
    private Long size;

    /**
     * Chemin physique ou logique.
     * Exemple :
     * uploads/students/2026/
     */
    @Column(nullable = false, length = 500)
    private String path;

    /**
     * Extension.
     * Exemple :
     * jpg
     * png
     * pdf
     */
    @Column(length = 20)
    private String extension;

    /**
     * Hash SHA-256 ou MD5 permettant d'eviter les doublons.
     */
    @Column(length = 128)
    private String checksum;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
