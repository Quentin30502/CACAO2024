package abstraction.eq4Transformateur1;

import java.util.HashMap;
import java.util.List;
import abstraction.eqXRomu.general.Variable;
import abstraction.eqXRomu.clients.ClientFinal;
import abstraction.eqXRomu.contratsCadres.ExemplaireContratCadre;
import abstraction.eqXRomu.filiere.IDistributeurChocolatDeMarque;
import abstraction.eqXRomu.general.Journal;
import abstraction.eqXRomu.produits.ChocolatDeMarque;
import abstraction.eqXRomu.produits.Gamme;

/**
 * @author yannig_charonnat
 */

public class Transformateur1Distribution extends Transformateur1AcheteurCCadre implements IDistributeurChocolatDeMarque {
    
    private double pourcentageVenteDirecte;
    private Journal journalVD;

    public Transformateur1Distribution() {
    	super();
        this.pourcentageVenteDirecte = 0.15;
        this.journalVD = new Journal("Ventes Directes", this);
    }

    public List<Journal> getJournaux() {
		List<Journal> jx=super.getJournaux();
		jx.add(journalVD);
		return jx;
	}

	@Override
	public double prix(ChocolatDeMarque choco) {
		double prixMoyen = 0;
		int nbPrix = 0;
		
		for(ExemplaireContratCadre c :this.contratsEnCours) {
			if(c.getProduit() == choco) {
				for(double p: c.getListePrix()) {
					prixMoyen += p;
					nbPrix++;
				}
			}
		}
		if (nbPrix != 0) {
			return prixMoyen/nbPrix * 1.1;
		} return PRIX_DEFAUT * 1.1;
	}

	@Override
	public double quantiteEnVente(ChocolatDeMarque choco, int crypto) {
		if(choco.getGamme()==Gamme.HQ && this.stockChocoMarque.keySet().contains(choco)) {
			return this.stockChocoMarque.get(choco).getValeur() * this.pourcentageVenteDirecte;
		}
		return 0;
	}

	@Override
	public double quantiteEnVenteTG(ChocolatDeMarque choco, int crypto) {
		return this.quantiteEnVente(choco, crypto) * 0.099; //maximum
	}

	@Override
	public void vendre(ClientFinal client, ChocolatDeMarque choco, double quantite, double montant, int crypto) {
		if (this.stockChocoMarque!=null && this.stockChocoMarque.keySet().contains(choco)) {
			this.stockChocoMarque.get(choco).setValeur(this, this.stockChocoMarque.get(choco).getValeur()-quantite);
			this.totalStocksChocoMarque.retirer(this,  quantite, cryptogramme);
			this.journalVD.ajouter("vente de "+quantite+" T de "+choco+" pour un prix de "+montant+" !");
		}
		
	}

	@Override
	public void notificationRayonVide(ChocolatDeMarque choco, int crypto) {
		this.pourcentageVenteDirecte = Math.min(0.75, this.pourcentageVenteDirecte*1.05); // A modifier pour plus de réalisme mais là on est les seuls distributeurs...
		this.journalVD.ajouter("Plus de chocolat : "+choco+" en rayon");
	}
}