package abstraction.eq1Producteur1;






/*
 *///////////////////////////////////////YOUSSEF BEN ABDELJELIL//////////////////////////////
////////////////////////////////////////////////////////////////////////////////*/
public class Ouvrier extends Producteur1Acteur {
	public double anciennete;
	public double rendement;
	public double salaire;
	public boolean isEquitable;
	public boolean isForme;
	public Ouvrier(double anciennete,double rendement,double salaire) {
		
		this.rendement=rendement;
		this.anciennete=anciennete;
		this.salaire=salaire;
		this.isForme = isForme;
	}
	public double getAnciennete() {
		return anciennete;
	}
	public void setAnciennete(double anciennete) {
		this.anciennete = anciennete;
	}
	public double getRendement() {
		return rendement;
	}
	public void setRendement(double rendement) {
		this.rendement = rendement;
	}
	public double getSalaire() {
		return salaire;
	}
	public void setSalaire(double salaire) {
		this.salaire = salaire;
	}
	public boolean getIsEquitable() {
		return this.isEquitable;
		
	}
	public void setIsEquitable(boolean IsEquitable) {
		this.isEquitable=IsEquitable;
		
	}
	public boolean getIsForme() {
		return this.isForme;
	}
	public void setIsForme(boolean bool) {
		 this.isForme=bool;
	}
		
		
	
	public void formation (Ouvrier o ) {
		double ancienneteMin =10;
		double augmentationRendement=0.5;
		double augmentationSalaire=0.1*this.getSalaire();
		if ( this.getAnciennete()<= ancienneteMin) {
			this.setRendement(this.getRendement()+ augmentationRendement);
			this.setSalaire(this.getSalaire()+augmentationSalaire);
			this.setIsForme(true);
		     int size = this.soldeParStep.size();
		     double solde = this.soldeParStep.get(size);
		     double k =5;
		     this.soldeParStep.add(size-1, solde - k);
		    	 
		}
		
		
		
	}
	
	

}
