package pharmacie.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import pharmacie.entity.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégrité des données dans la base.
 * On utilise saveAndFlush() pour forcer l'écriture en BD
 * et déclencher les contraintes.
 */
@DataJpaTest
class RepositoryIntegrityTest {

    @Autowired
    private CategorieRepository categorieRepository;
    @Autowired
    private MedicamentRepository medicamentRepository;
    @Autowired
    private DispensaireRepository dispensaireRepository;
    @Autowired
    private CommandeRepository commandeRepository;
    @Autowired
    private LigneRepository ligneRepository;

    @Test
    void unMedicamentSansCategorieEstInterdit() {
        // On essaie de sauvegarder un médicament sans catégorie
        Medicament m = new Medicament();
        m.setNom("Doliprane");

        // Ca doit planter car categorie_code est NOT NULL
        assertThrows(DataIntegrityViolationException.class, () -> {
            medicamentRepository.saveAndFlush(m);
        });
    }

    @Test
    void detruireLaCategorieSupprimeSesMedicaments() {
        // On crée une catégorie avec un médicament
        Categorie c = new Categorie();
        c.setLibelle("Antalgiques");

        Medicament m = new Medicament();
        m.setNom("Doliprane");
        m.setCategorie(c);

        // On ajoute le médicament dans la liste de la catégorie (cascade)
        c.getMedicaments().add(m);

        // On sauvegarde la catégorie, le médicament est créé par cascade
        c = categorieRepository.saveAndFlush(c);
        m = c.getMedicaments().get(0);
        Integer reference = m.getReference();

        assertNotNull(reference, "Le médicament doit avoir une référence auto-générée");

        // On supprime la catégorie
        categorieRepository.delete(c);
        categorieRepository.flush();

        // Le médicament doit avoir été supprimé aussi (CascadeType.ALL)
        Optional<Medicament> resultat = medicamentRepository.findById(reference);
        assertFalse(resultat.isPresent(), "Le médicament doit être supprimé avec sa catégorie");
    }

    @Test
    void uneCommandeSansDispensaireEstInterdite() {
        // On essaie de créer une commande sans dispensaire
        Commande commande = new Commande();
        commande.setDestinataire("Mr Smith");

        // Ca doit planter car dispensaire_code est NOT NULL
        assertThrows(DataIntegrityViolationException.class, () -> {
            commandeRepository.saveAndFlush(commande);
        });
    }

    @Test
    void detruireLeDispensaireSupprimeSesCommandes() {
        // On crée un dispensaire avec une commande
        Dispensaire d = new Dispensaire();
        d.setCode("D001");
        d.setNom("Dispensaire Central");

        Commande commande = new Commande();
        commande.setDestinataire("Mr Smith");
        commande.setDispensaire(d);

        // On ajoute la commande dans la liste du dispensaire (cascade)
        d.getCommandes().add(commande);

        d = dispensaireRepository.saveAndFlush(d);
        commande = d.getCommandes().get(0);
        Integer numero = commande.getNumero();

        assertNotNull(numero, "La commande doit avoir un numéro auto-généré");

        // On supprime le dispensaire
        dispensaireRepository.delete(d);
        dispensaireRepository.flush();

        // La commande doit être supprimée aussi (CascadeType.ALL)
        Optional<Commande> resultat = commandeRepository.findById(numero);
        assertFalse(resultat.isPresent(), "La commande doit être supprimée avec son dispensaire");
    }

    @Test
    void uneLigneSansCommandeOuMedicamentEstInterdite() {
        // On essaie de créer une ligne sans commande ni médicament
        Ligne ligne = new Ligne();
        ligne.setQuantite(10);

        // Ca doit planter car commande_numero et medicament_reference sont NOT NULL
        assertThrows(DataIntegrityViolationException.class, () -> {
            ligneRepository.saveAndFlush(ligne);
        });
    }

    @Test
    void medicamentDupliqueDansUneMemeCommandeEstInterdit() {
        // On prépare les données de base
        Categorie cat = new Categorie();
        cat.setLibelle("Test Cat");
        cat = categorieRepository.saveAndFlush(cat);

        Medicament med = new Medicament();
        med.setNom("Test Med");
        med.setCategorie(cat);
        med = medicamentRepository.saveAndFlush(med);

        Dispensaire d = new Dispensaire();
        d.setCode("D002");
        d.setNom("Test Disp");
        d = dispensaireRepository.saveAndFlush(d);

        Commande cmd = new Commande();
        cmd.setDispensaire(d);
        cmd = commandeRepository.saveAndFlush(cmd);

        // Première ligne : on met le médicament dans la commande
        Ligne ligne1 = new Ligne();
        ligne1.setCommande(cmd);
        ligne1.setMedicament(med);
        ligne1.setQuantite(5);
        ligneRepository.saveAndFlush(ligne1);

        // Deuxième ligne : même commande + même médicament = interdit !
        // (contrainte d'unicité sur (commande_numero, medicament_reference))
        Ligne ligneDupliquee = new Ligne();
        ligneDupliquee.setCommande(cmd);
        ligneDupliquee.setMedicament(med);
        ligneDupliquee.setQuantite(2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            ligneRepository.saveAndFlush(ligneDupliquee);
        });
    }

    @Test
    void testOrphanRemovalLigne() {
        // On prépare les données de base
        Categorie cat = new Categorie();
        cat.setLibelle("Orphan Cat");
        cat = categorieRepository.saveAndFlush(cat);

        Medicament med = new Medicament();
        med.setNom("Orphan Med");
        med.setCategorie(cat);
        med = medicamentRepository.saveAndFlush(med);

        Dispensaire d = new Dispensaire();
        d.setCode("D003");
        d.setNom("Orphan Disp");
        d = dispensaireRepository.saveAndFlush(d);

        Commande cmd = new Commande();
        cmd.setDispensaire(d);

        // On crée une ligne et on l'ajoute à la commande
        Ligne ligne = new Ligne();
        ligne.setCommande(cmd);
        ligne.setMedicament(med);
        ligne.setQuantite(1);
        cmd.getLignes().add(ligne);

        // On sauvegarde la commande, la ligne est créée par cascade
        cmd = commandeRepository.saveAndFlush(cmd);

        Ligne ligneSauvee = cmd.getLignes().get(0);
        Integer ligneId = ligneSauvee.getId();
        assertNotNull(ligneId);

        // On retire la ligne de la collection
        cmd.getLignes().remove(ligneSauvee);
        commandeRepository.saveAndFlush(cmd);

        // Grâce à orphanRemoval=true, la ligne doit être supprimée de la BD
        Optional<Ligne> resultat = ligneRepository.findById(ligneId);
        assertFalse(resultat.isPresent(), "La ligne orpheline doit être supprimée automatiquement");
    }

    @Test
    void neDetruitPasUneCategorieSiSesMedicamentsSontCommandes() {
        // On récupère la catégorie 1 qui a des médicaments dans des commandes (via
        // data.sql)
        Categorie c = categorieRepository.findById(1).orElseThrow();

        // Elle doit avoir 10 médicaments (insérés par data.sql)
        int nbMedicaments = c.getMedicaments().size();
        assertEquals(10, nbMedicaments, "La catégorie 1 doit avoir 10 médicaments");

        // On essaie de supprimer la catégorie
        // Ca doit échouer car ses médicaments sont référencés dans des lignes de
        // commande
        try {
            categorieRepository.delete(c);
            categorieRepository.flush();
            fail("On ne devrait pas pouvoir supprimer une catégorie dont les médicaments sont commandés");
        } catch (DataIntegrityViolationException e) {
            // C'est normal, la contrainte de clé étrangère empêche la suppression
        }
    }
}
