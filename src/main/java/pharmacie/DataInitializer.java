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
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final FournisseurRepository fournisseurRepository;
    private final CategorieRepository categorieRepository;
    private final MedicamentRepository medicamentRepository;

    public DataInitializer(FournisseurRepository fournisseurRepository,
            CategorieRepository categorieRepository,
            MedicamentRepository medicamentRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.categorieRepository = categorieRepository;
        this.medicamentRepository = medicamentRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Initialisation des données (DataInitializer) ===");

        Fournisseur pfizer = new Fournisseur("Pfizer");
        pfizer.setEmail("jonniaux.math+pfizer@gmail.com");
        pfizer.setTelephone("0102030405");
        pfizer = fournisseurRepository.save(pfizer);
        System.out.println("Fournisseur créé : " + pfizer.getNom());

        Fournisseur sanofi = new Fournisseur("Sanofi");
        sanofi.setEmail("jonniaux.math+sanofi@gmail.com");
        sanofi.setTelephone("0607080910");
        sanofi = fournisseurRepository.save(sanofi);
        System.out.println("Fournisseur créé : " + sanofi.getNom());

        Categorie cardiologie = new Categorie("Cardiologie");
        cardiologie.setDescription("Médicaments pour le coeur");
        cardiologie.setFournisseurs(Set.of(pfizer, sanofi));
        cardiologie = categorieRepository.save(cardiologie);
        System.out.println("Catégorie créée : " + cardiologie.getLibelle());

        // stock faible (5) pour un seuil de 10 -> devrait déclencher le réappro
        Medicament kardegic = new Medicament("Kardegic", cardiologie);
        kardegic.setUnitesEnStock(5);
        kardegic.setNiveauDeReappro(10);
        medicamentRepository.save(kardegic);
        System.out.println("Médicament créé : " + kardegic.getNom() + " (stock=" + kardegic.getUnitesEnStock() + ")");

        // stock suffisant, pas de réappro
        Medicament tahor = new Medicament("Tahor", cardiologie);
        tahor.setUnitesEnStock(20);
        tahor.setNiveauDeReappro(5);
        medicamentRepository.save(tahor);
        System.out.println("Médicament créé : " + tahor.getNom() + " (stock=" + tahor.getUnitesEnStock() + ")");

        System.out.println("=== Fin DataInitializer ===");
    }
}
