package pharmacie;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import pharmacie.dao.CategorieRepository;
import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Component
@Order(2)
public class MiniProjetDataInit implements CommandLineRunner {

    private final FournisseurRepository fournisseurRepository;
    private final CategorieRepository categorieRepository;
    private final MedicamentRepository medicamentRepository;

    public MiniProjetDataInit(FournisseurRepository fournisseurRepository,
            CategorieRepository categorieRepository,
            MedicamentRepository medicamentRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.categorieRepository = categorieRepository;
        this.medicamentRepository = medicamentRepository;
    }

    @Override
    public void run(String... args) {
        // Ne pas réinsérer si les données existent déjà
        if (medicamentRepository.count() > 0) {
            System.out.println("=== MiniProjetDataInit : données déjà présentes, skip ===");
            return;
        }
        System.out.println("=== Initialisation des données MiniProjet ===");

        Fournisseur laboA = new Fournisseur("Labo A");
        laboA.setEmail("jonniaux.math+laboa@gmail.com");
        laboA.setTelephone("0111111111");
        laboA = fournisseurRepository.save(laboA);
        System.out.println("Fournisseur créé : " + laboA.getNom());

        Fournisseur laboB = new Fournisseur("Labo B");
        laboB.setEmail("jonniaux.math+labob@gmail.com");
        laboB.setTelephone("0222222222");
        laboB = fournisseurRepository.save(laboB);
        System.out.println("Fournisseur créé : " + laboB.getNom());

        // On relie la catégorie aux DEUX fournisseurs
        Categorie douleur = new Categorie("Douleur");
        douleur.setDescription("Médicaments contre la douleur");
        douleur.setFournisseurs(Set.of(laboA, laboB));
        douleur = categorieRepository.save(douleur);
        System.out.println("Catégorie créée : " + douleur.getLibelle());

        // stock faible pour tester le réapprovisionnement
        Medicament doliprane = new Medicament("Doliprane", douleur);
        doliprane.setUnitesEnStock(3);
        doliprane.setNiveauDeReappro(15);
        medicamentRepository.save(doliprane);
        System.out.println("Médicament créé : " + doliprane.getNom()
                + " (stock=" + doliprane.getUnitesEnStock() + ", seuil=" + doliprane.getNiveauDeReappro() + ")");

        Medicament ibuprofene = new Medicament("Ibuprofène", douleur);
        ibuprofene.setUnitesEnStock(2);
        ibuprofene.setNiveauDeReappro(10);
        medicamentRepository.save(ibuprofene);
        System.out.println("Médicament créé : " + ibuprofene.getNom()
                + " (stock=" + ibuprofene.getUnitesEnStock() + ", seuil=" + ibuprofene.getNiveauDeReappro() + ")");

        System.out.println("=== Fin MiniProjetDataInit ===");
    }
}
