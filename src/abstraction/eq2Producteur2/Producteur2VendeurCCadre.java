package abstraction.eq2Producteur2;

import java.awt.Color;

import java.util.LinkedList;
import java.util.List;

import abstraction.eqXRomu.bourseCacao.BourseCacao;
import abstraction.eqXRomu.contratsCadres.Echeancier;
import abstraction.eqXRomu.contratsCadres.ExemplaireContratCadre;
import abstraction.eqXRomu.contratsCadres.IAcheteurContratCadre;
import abstraction.eqXRomu.contratsCadres.IVendeurContratCadre;
import abstraction.eqXRomu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eqXRomu.filiere.Filiere;
import abstraction.eqXRomu.general.Journal;
import abstraction.eqXRomu.produits.Feve;
import abstraction.eqXRomu.produits.IProduit;


/** classe Contrat Cadre
 * @author Maxime
 * @author Prof
 */

public abstract class Producteur2VendeurCCadre extends Producteur2VendeurBourse implements IVendeurContratCadre {

	private static int PRIX_DEFAUT = 4500;
	
	private SuperviseurVentesContratCadre supCC;
	private List<ExemplaireContratCadre> contratsEnCours;
	private List<ExemplaireContratCadre> contratsTermines;
	protected Journal journalCC;
	protected double quantiteVendueCC;
	protected int etapeCC;
	protected double prix_contrat;

	public Producteur2VendeurCCadre() {
		super();
		this.contratsEnCours=new LinkedList<ExemplaireContratCadre>();
		this.contratsTermines=new LinkedList<ExemplaireContratCadre>();
		this.journalCC = new Journal(this.getNom()+" journal CC", this);
	}
	
	public void initialiser() {
		super.initialiser();
		this.etapeCC = 0;
		this.supCC = (SuperviseurVentesContratCadre)(Filiere.LA_FILIERE.getActeur("Sup.CCadre"));
	}
	
	/** next
	 * @author Maxime
	 */
	
	public void next() {
		super.next();
		this.journalCC.ajouter("=== STEP "+Filiere.LA_FILIERE.getEtape()+" ====================");
		for (Feve f : stock.keySet()) { // pas forcement equitable : on avise si on lance un contrat cadre pour tout type de feve
			if (stock.get(f)-restantDu(f)>1200) { // au moins 100 tonnes par step pendant 6 mois
				this.journalCC.ajouter("   "+f+" suffisamment en stock pour passer un CC");
				double parStep = Math.max(100, (stock.get(f)-restantDu(f))/24); // au moins 100, et pas plus que la moitie de nos possibilites divisees par 2
				Echeancier e = new Echeancier(Filiere.LA_FILIERE.getEtape()+1, 12, parStep);
				List<IAcheteurContratCadre> acheteurs = supCC.getAcheteurs(f);
				if (acheteurs.size()>0) {
					IAcheteurContratCadre acheteur = acheteurs.get(Filiere.random.nextInt(acheteurs.size()));
					journalCC.ajouter("   "+acheteur.getNom()+" retenu comme acheteur parmi "+acheteurs.size()+" acheteurs potentiels");
					ExemplaireContratCadre contrat = supCC.demandeVendeur(acheteur, this, f, e, cryptogramme, false);
					if (contrat==null) {
						journalCC.ajouter(Color.RED, Color.white,"   echec des negociations");
					} else {
						this.contratsEnCours.add(contrat);
						this.prix_contrat = contrat.getPrix();
						journalCC.ajouter(Color.GREEN, acheteur.getColor(), "   contrat signe");
					}
				} else {
					journalCC.ajouter("pas d'acheteur");
				}
			}
		}
		
		// On archive les contrats termines
		for (ExemplaireContratCadre c : this.contratsEnCours) {
			if (c.getQuantiteRestantALivrer()==0.0) {
				this.contratsTermines.add(c);
			}
		}
		for (ExemplaireContratCadre c : this.contratsTermines) {
			this.contratsEnCours.remove(c);
		}
		this.journalCC.ajouter("=================================");
	}


	/** Retourne la quantité de fèves qui doivent encore être livrées
	 * @author Maxime
	 */
	public double restantDu(Feve f) {
		double res=0;
		for (ExemplaireContratCadre c : this.contratsEnCours) {
			if (c.getProduit().equals(f)) {
				res+=c.getQuantiteRestantALivrer();
			}
		}
		return res;
	}

	/** prix  
	 * @author Maxime
	 */
	public double prix(Feve f) {
		double res=0;
		List<Double> lesPrix = new LinkedList<Double>();
		for (ExemplaireContratCadre c : this.contratsEnCours) {
			if (c.getProduit().equals(f)) {
				lesPrix.add(c.getPrix());
			}
		}
		for (ExemplaireContratCadre c : this.contratsTermines) {
			if (c.getProduit().equals(f)) {
				lesPrix.add(c.getPrix());
			}
		}
		if (lesPrix.size()>0) {
			double somme=0;
			for (Double d : lesPrix) {
				somme+=d;
			}
			res=somme/lesPrix.size();
		}
		return res;
	}
	
	/** Ajoute le journalCC aux autres journaux
	 * @author Maxime
	 */
	public List<Journal> getJournaux() {
		List<Journal> jx=super.getJournaux();
		jx.add(journalCC);
		return jx;
	}

	/** Indique par un booléen si on a assez de fèves pour passer un contrat cadre
	 * @author Maxime
	 */
	public boolean vend(IProduit produit) {
		return produit.getType().equals("Feve") && stock.get((Feve)produit)-restantDu((Feve)produit)>1200;
	}
	
	public double getNbTonnesVenduesCC() {
		double nb = this.quantiteVendueCC;
		
		if (Filiere.LA_FILIERE.getEtape() != this.etapeCC) {
			// On remet à 0 la quantité vendue à chaque tour (on souhaite suivre la vente par tour)
			this.quantiteVendueCC = 0 ;
			this.etapeCC = Filiere.LA_FILIERE.getEtape();
		}
		return nb;
	}

	/** contre proposition du vendeur
	 * @author Maxime
	 */
	public Echeancier contrePropositionDuVendeur(ExemplaireContratCadre contrat) {
		journalCC.ajouter("      contreProposition("+contrat.getProduit()+" avec echeancier "+contrat.getEcheancier());
		Echeancier ec = contrat.getEcheancier();
		IProduit produit = contrat.getProduit();
		Echeancier res = ec;
		boolean acceptable = produit.getType().equals("Feve")
				&& ec.getQuantiteTotale()>=1200  // au moins 100 tonnes par step pendant 6 mois
				&& ec.getStepFin()-ec.getStepDebut()>=11   // duree totale d'au moins 12 etapes
				&& ec.getStepDebut()<Filiere.LA_FILIERE.getEtape()+8 // ca doit demarrer dans moins de 4 mois
				&& ec.getQuantiteTotale()<stock.get((Feve)produit)-restantDu((Feve)produit);
				if (!acceptable) {
					if (!produit.getType().equals("Feve") || stock.get((Feve)produit)-restantDu((Feve)produit)<1200) {
						if (!produit.getType().equals("Feve")) {
							journalCC.ajouter("      ce n'est pas une feve : je retourne null");
						} else {
							journalCC.ajouter("      je n'ai que "+(stock.get((Feve)produit)-restantDu((Feve)produit))+" de disponible (moins de 1200) : je retourne null");
						}
						return null;
					}
					if (ec.getQuantiteTotale()<=stock.get((Feve)produit)-restantDu((Feve)produit)) {
						journalCC.ajouter("      je retourne "+new Echeancier(Filiere.LA_FILIERE.getEtape()+1, 12,  (int)(ec.getQuantiteTotale()/12)));
						return new Echeancier(Filiere.LA_FILIERE.getEtape()+1, 12,  (int)(ec.getQuantiteTotale()/12));
					} else {
						journalCC.ajouter("      je retourne "+new Echeancier(Filiere.LA_FILIERE.getEtape()+1, 12,  (int)((stock.get((Feve)produit)-restantDu((Feve)produit)/12))));
						return new Echeancier(Filiere.LA_FILIERE.getEtape()+1, 12,  (int)((stock.get((Feve)produit)-restantDu((Feve)produit)/12)));
					}
				}
				journalCC.ajouter("      j'accepte l'echeancier");
				return res;
	}


	/** Proposition de prix contrat cadre
	 * @author Maxime
	 */
	public double propositionPrix(ExemplaireContratCadre contrat) {
		if (!contrat.getProduit().getType().equals("Feve")) {
			return 0; // ne peut pas etre le cas normalement 
		}
		BourseCacao bourse = (BourseCacao)(Filiere.LA_FILIERE.getActeur("BourseCacao"));
		double cours = ((Feve)(contrat.getProduit())).isEquitable() ? 0.0 : bourse.getCours((Feve)contrat.getProduit()).getValeur();
		double prixCC = prix((Feve)contrat.getProduit());
		if (prixCC==0.0) {
			PRIX_DEFAUT=(int)(PRIX_DEFAUT*0.98); // on enleve 2% tant qu'on n'a pas passe un contrat
		}
		double res = prixCC>cours ? prixCC*1.25 : (cours<PRIX_DEFAUT ? PRIX_DEFAUT : (int)(cours*1.5));
		journalCC.ajouter("      propositionPrix retourne "+res);
		return res;
	}


	/** Contre proposition de prix du vendeur pour un
	 * @author Maxime
	 */
	public double contrePropositionPrixVendeur(ExemplaireContratCadre contrat) {
		if (!contrat.getProduit().getType().equals("Feve")) {
			return 0; // ne peut pas etre le cas normalement 
		}
		List<Double> prix = contrat.getListePrix();
		if (prix.get(prix.size()-1)>=0.995*prix.get(0)) {
			journalCC.ajouter("      contrePropose le prix demande : "+contrat.getPrix());
			return contrat.getPrix();
		} else {
			int percent = (int)(100* Math.pow((contrat.getPrix()/prix.get(0)), prix.size()));
			int alea = Filiere.random.nextInt(100);
			if (alea< percent) { // d'autant moins de chance d'accepter que le prix est loin de ce qu'on proposait
				if (Filiere.random.nextInt(100)<20) { // 1 fois sur 5 on accepte
					journalCC.ajouter("      contrePropose le prix demande : "+contrat.getPrix());
					return contrat.getPrix();
				} else {
					double res = (prix.get(prix.size()-2)+contrat.getPrix())/2.0; // la mmoyenne des deux derniers prix
					journalCC.ajouter("      contreproposition("+contrat.getPrix()+") retourne "+res);
					return res;
				}
			} else {
				journalCC.ajouter("      contreproposition("+contrat.getPrix()+") retourne "+prix.get(0)*1.05);
				return prix.get(0)*1.05;
	 		}
		}
	}

	public void notificationNouveauContratCadre(ExemplaireContratCadre contrat) {
		this.contratsEnCours.add(contrat);
	}

	/** Retire les fèves du stock pour les livrer au client
	 * @author Maxime
	 */
	public double livrer(IProduit produit, double quantite, ExemplaireContratCadre contrat) {
		double stockActuel = stock.get(produit);
		double aLivre = Math.min(quantite, stockActuel);
		this.stock_a_vendre((Feve) produit, quantite);
		journalCC.ajouter("   Livraison de "+aLivre+" T de "+produit+" sur "+quantite+" exigees pour contrat "+contrat.getNumero());
		this.quantiteVendueCC = this.quantiteVendueCC + aLivre;
		return aLivre;
	} 
}
