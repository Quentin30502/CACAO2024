package abstraction.eq3Producteur3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import abstraction.eqXRomu.contratsCadres.ContratCadre;
import abstraction.eqXRomu.contratsCadres.Echeancier;
import abstraction.eqXRomu.contratsCadres.ExemplaireContratCadre;
import abstraction.eqXRomu.contratsCadres.IAcheteurContratCadre;
import abstraction.eqXRomu.contratsCadres.IVendeurContratCadre;
import abstraction.eqXRomu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eqXRomu.filiere.Filiere;
import abstraction.eqXRomu.filiere.IActeur;
import abstraction.eqXRomu.produits.Feve;
import abstraction.eqXRomu.produits.Gamme;
import abstraction.eqXRomu.produits.IProduit;

public class Producteur3VendeurContratCadre extends Producteur3VendeurBourse implements IVendeurContratCadre {
	//@youssef
	private LinkedList<ExemplaireContratCadre> contratsEnCours = new LinkedList<>();
	private SuperviseurVentesContratCadre superviseur;
	private ExemplaireContratCadre contr;
	//@author Alexis
	private int itQ; //compteur du step de négo
	private double prixNego;
	private double livre = 0;
	private double nonLivre = 0;
	
	/**
	 * @author Arthur
	 * @param IProduit produit (produit potentiellment que l'on veut vendre ou non)
	 * @return boolean (reponse si on vend ou pas)
	 * La fonction renvoie si oui ou non, on veut vendre du produit proposer en CC (oui si feve HQ et MQ)
	 */
	public boolean vend(IProduit produit) {
		//On accepte les contrats cadres sur le HQ et MQ en V1
		if ((produit instanceof Feve)&&((((Feve)produit).getGamme() == Gamme.HQ)||(((Feve)produit).getGamme() == Gamme.MQ))) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @author Youssef (modification Arthur)
	 * Initalise le super et invoque le superviseur
	 */
	public void initialiser() {
		super.initialiser();
		//On appelle le superviseur de la filiere
		superviseur = (SuperviseurVentesContratCadre) Filiere.LA_FILIERE.getActeur("Sup."+(SuperviseurVentesContratCadre.NB_SUPRVISEURS_CONTRAT_CADRE>1?SuperviseurVentesContratCadre.NB_SUPRVISEURS_CONTRAT_CADRE+"":"")+"CCadre");
		itQ = 0;
		prixNego = 0;
	}
	
	/**
	 * @author Youssef
	 * Fait le next du super et lance des CC
	 */
	public void next() {
        super.next();
        //on lance de nouveaux contrats (a verifier)
        proposerContrats();
        if (Filiere.LA_FILIERE.getEtape() % 100 == 98) {
        	this.journal_contrat_cadre.ajouter("Taux de livraisons partielles : "+nonLivre/livre);
        }
    }
	
	/**
	 * @author mammouYoussef (modification par Arthur)
	 * Fonction qui lance des CC selon la feve et notre capacite et fournir
	 */
	public void proposerContrats() {
	
	    // Créer une liste de fèves de qualité MQ et HQ uniquement
	    List<Feve> feves = new ArrayList<Feve>();
	    for (Feve feve : Feve.values()) {
	        if (feve.getGamme() == Gamme.MQ || feve.getGamme() == Gamme.HQ) {
	            feves.add(feve);
	        }
	    }
	    for (Feve f : feves) { 
	    	//pour tous les acheteurs de chaque feves on propose un echeancier de 10 step
	        List<IAcheteurContratCadre> acheteurs = superviseur.getAcheteurs(f);
	        Set<IAcheteurContratCadre> acheteurSet = new HashSet<IAcheteurContratCadre>(acheteurs);
	        for (IAcheteurContratCadre acheteur : acheteurSet) {
	        	int dureeStep = 10; //duree du CC en step
		        LinkedList<Double> quantiteDispo = quantiteDisponibleFutur(f,Filiere.LA_FILIERE.getEtape(),dureeStep+Filiere.LA_FILIERE.getEtape());
		        double quantiteDisponible = quantiteDispo.stream().reduce(Double::sum).get();
		        if (quantiteDisponible > SuperviseurVentesContratCadre.QUANTITE_MIN_ECHEANCIER) {
                    //on propose de livrer a chaque step la quantite qui nous reste apres livraison des autres CC
		        	Echeancier echeancier = new Echeancier(Filiere.LA_FILIERE.getEtape()+1, quantiteDispo); // Crée un échéancier avec des livraisons réparties sur 10 étapes (à modifier)
                    contr = superviseur.demandeVendeur(acheteur, this, f, echeancier, this.cryptogramme, false); // Démarre la négociation
                    if (contr != null) {
                    	//la fonction notificationNouveauContratCadre n'étant pas appellée, on fait son travail ici
                    	this.journal_contrat_cadre.ajouter("Contrat cadre n°"+contr.getNumero()+" avec "+contr.getAcheteur().getNom()+" : "+contr.getQuantiteTotale()+" T de "+contr.getProduit()+" a "+contr.getPrix()+" E/T");	
                		this.contratsEnCours.add(contr);
                		this.setContratsEnCours();
                    }
		        }
            }
	    }
	}
	
	
	/**
	 * @author mammouYoussef
	 * Nettoie la liste des contrats en cours, en éliminant ceux dont les obligations 
	 * de livraison ont été entièrement satisfaites
	 */
	public void setContratsEnCours() {
	    LinkedList<ExemplaireContratCadre> contratsAConserver = new LinkedList<>();
	    for (ExemplaireContratCadre contrat : contratsEnCours) {
	        if (contrat.getQuantiteRestantALivrer() > 0) {
	            contratsAConserver.add(contrat);
	        }
	    }
	    contratsEnCours = contratsAConserver;
	}


	/**
	 * @author mammouYoussef (et modification Arthur et Alexis pour quantiteFuture)
	 * @param Feve f,int step (feve a laquelle on s'interesse et step d'etude)
	 * @return double quantiteDisponible (quantite qu'on l'on peut disposer pour les CC pas encore négociés)
	 * Calcule et retourne la quantité disponible d'une fève spécifique pour de nouveaux contrats, en prenant en compte les engagements existants
	 * Le calcul se fait a un step donné afin de prendre en compte la saisonnalité des plantations notamment
	 */
	 private double quantiteDisponiblePourNouveauContrat(Feve f, int step) {		 	
		 	double quantiteDisponible = 0.0; // Valeur par défaut
	        if (quantiteFuture().containsKey(f)) {
	        	//La quantite disponible de base correspond a ce que l'on produit à l'étape d'après (on prend le min du tour d'avant et d'apres le step qui nous interesse pour eviter les livraisons partielles)
	        	quantiteDisponible = Math.min(quantiteFuture().get(f)/coeff(Filiere.LA_FILIERE.getEtape())*coeff(step-2),quantiteFuture().get(f)/coeff(Filiere.LA_FILIERE.getEtape())*coeff(step));
	        }

	        for (ExemplaireContratCadre contrat : contratsEnCours) {
	            if (contrat.getProduit().equals(f)) {
	            	//il faut ensuite enlever ce que l'on doit livrer pour avoir la quantite disponible pour d'autres CC
	                //On prend la quantité a livrer au tour d'après pour prendre en compte la première livraison
	            	quantiteDisponible -= contrat.getEcheancier().getQuantite(step);
	            }
	        }
	        if (quantiteDisponible < 0) {
	        	//la quantite disponible ne peut pas etre negative
	        	quantiteDisponible = 0;
	        }
	        return quantiteDisponible;
	    }
	 
	 /**
	  * @author Arthur
	  * @param Feve f, int stepFin (feve a laquelle on s'interesse et step de fin du CC)
	  * @return quantiteDispo (dictionnaire qui associe a chaque step la quantite dispo pour CC)
	  * Fonction qui sert a savoir la quantite dispo pour CC dans le futur afin de faire des CC sans avoir de probleme avec la saisonnalité
	  */
	 private LinkedList<Double> quantiteDisponibleFutur(Feve f, int stepDebut, int stepFin) {
		 LinkedList<Double> quantiteDispo = new LinkedList<Double>();
		 for (int step = stepDebut; step <= stepFin; step++) {
			 quantiteDispo.add(quantiteDisponiblePourNouveauContrat(f,step));
		 }
		 return quantiteDispo;
	 }
	 
	 /**
	  * @author mammouYoussef (et modification Arthur)
	  * @param ExemplaireContratCadre contrat (contrat actuel de la négociation)
	  * @return Echeancier nouvelEcheancier (écheancier proposé en retour)
	  * Propose un echeancier dans le but de satisfaire au mieux celui proposé par le vendeur tout en prenant en compte nos capacités à fournir ce qui est demandé
	  */
	 public Echeancier contrePropositionDuVendeur(ExemplaireContratCadre contrat) {
		    Feve f = (Feve) contrat.getProduit();
		    Echeancier echeancierPropose = contrat.getEcheancier();
		    Echeancier nouvelEcheancier = new Echeancier(echeancierPropose.getStepDebut());
		    //on estime que la quantite disponible est similaire a chaque step en V1
		    LinkedList<Double> quantiteDisponible = quantiteDisponibleFutur(f,echeancierPropose.getStepDebut(),echeancierPropose.getStepFin());

		    for (int step = echeancierPropose.getStepDebut(); step <= echeancierPropose.getStepFin(); step++) {
		    	int i = step - echeancierPropose.getStepDebut();
		        double quantiteDemandee = echeancierPropose.getQuantite(step);

		        if (quantiteDisponible.get(i) >= quantiteDemandee) {
		        	//si on peut fournir ce qui est demande, on le fait
		            nouvelEcheancier.ajouter(quantiteDemandee);
		        } else {
		        	//sinon on propose de fournir ce que l'on peut
		            nouvelEcheancier.ajouter(quantiteDisponible.get(i));
		        }
		    }
		    if (nouvelEcheancier.getQuantiteTotale()>= SuperviseurVentesContratCadre.QUANTITE_MIN_ECHEANCIER) {
		        return nouvelEcheancier;
		    } else {
		        return null; // Retourner null si la quantite totale est trop faible
		    }
		}
	 
	
	
	/**
	 * @author mammouYoussef
	 * @param ExemplaireContratCadre contrat (contrat actuel de la négociation)
	 * @return double prixBase (prix proposé a la tonne)
	 * Propose un prix de base du cacao en fonction de la feve du contrat
	 */
	public double propositionPrix(ExemplaireContratCadre contrat) {
		itQ = 0; //sert a compter le nombre d'aller-retour dans les negociations (ici c'est juste l'initialisation)
	    IProduit produit = contrat.getProduit();
	    if (!(produit instanceof Feve)) { return 0;}
	    
	    Feve feve = (Feve) produit;
	    double prixBase=coutRevient(feve,contrat.getQuantiteTotale());
	    //on fixe un prix de base selon la gamme
	     if (feve.getGamme() == Gamme.HQ) {
	        prixBase = Math.max(3000.0,prixBase);
	    } else if (feve.getGamme() == Gamme.MQ) {
	       prixBase = Math.max(prixBase, 1910.0);
	    }
	  // Ajustements selon équitable et bio
	      if (feve.isEquitable() && feve.isBio()) {
	           prixBase = Math.max(3400.0, prixBase); // Prix pour bio-équitable
	      } else if (feve.isEquitable()) {
	           prixBase = Math.max(3200.0, prixBase); // Prix pour équitable 
	      }
	      prixNego = prixBase * 1.2;
	      return prixNego; // Ajouter une marge de profit par exemple de 20% à modifier
	}

	/**
	 * @author mammouYoussef (et modification Alexis)
	 * @param ExemplaireContratCadre contrat (contrat actuel de la négociation)
	 * @return double prix (contre proposition du prix)
	 * On propose un nouveau prix (potentiellement le meme) suite a la contre-proposition faite par l'acheteur
	 */
	public double contrePropositionPrixVendeur(ExemplaireContratCadre contrat) {
		itQ += 1; //actualise le nombre d'aller-retour de negociations
	    IProduit produit = contrat.getProduit();
	    if (!(produit instanceof Feve)) {
	        return 0; }
	    double prixPropose = contrat.getPrix();
	    double prixMinimal= prixNego/1.2;
	    // Si le prix proposé est supérieur au prixMinimal, accepter le prix proposé
	    if (prixPropose > prixMinimal) {
	        return prixPropose;
	    } else {

	        // Sinon, retourner un prix qui tend vers prixMinimal au cours de la négociation
	        return prixMinimal*1.2 - itQ*prixMinimal*0.2/15;
	    }
	}


	/**
	 * @author Arthur
	 * @param ExemplaireContratCadre contrat (contrat actuel de la négociation)
	 * Prend en compte le nouveau contrat conclu 
	 */
	public void notificationNouveauContratCadre(ExemplaireContratCadre contrat) {
		//CC conclu, on l'affiche dans les journaux et on met a jour les variables pour estimer la quantite disponible de feve
		this.journal_contrat_cadre.ajouter("Nouveau contrat cadre signé: Contrat N°" + contrat.getNumero() +  ", avec " + contrat.getAcheteur().getNom() +  ", pour " + contrat.getQuantiteTotale() + " tonnes de " + contrat.getProduit() +    " à " + contrat.getPrix() + " €/T");
		this.contratsEnCours.add(contrat);
		this.setContratsEnCours();
	}

	/**
	 * @author Arthur
	 * @param Iproduit produit, double quantite, ExemplaireContratCadre contrat (données de la négociation)
	 * @return double (quantié que l'on va effectivement livrer)
	 * Renvoie la quantite livrée 
	 */
	public double livrer(IProduit produit, double quantite, ExemplaireContratCadre contrat) {
		double stock_inst = this.getQuantiteEnStock((Feve)produit, this.cryptogramme);
		if (quantite <= stock_inst) {
			//on verifie que l'on puisse fournir la quantite demande
			//il faut modifier les stocks suite a la vente
			this.setQuantiteEnStock((Feve)produit, stock_inst-quantite);
			this.journal_contrat_cadre.ajouter("Livraison totale : "+quantite+" T de feves "+((Feve)produit).getGamme()+" pour le CC n°"+contrat.getNumero());
			//on envoie ce que l'on a promis et on met a jour les variables
			ventefevecadre.put((Feve)contrat.getProduit(), quantite);
			livre += 1;
			return quantite;
		} else {
			//on ne peut pas tout fournir, on envoie tout le stock et met a jour les variables
			this.setQuantiteEnStock((Feve)produit, 0);
			this.journal_contrat_cadre.ajouter("Livraison partielle : "+stock_inst+" T de feves "+((Feve)produit).getGamme()+" pour le CC n°"+contrat.getNumero());
			ventefevecadre.put((Feve)contrat.getProduit(), stock_inst);
			livre += 1;
			nonLivre += 1;
			return stock_inst;
		}
	}
}
