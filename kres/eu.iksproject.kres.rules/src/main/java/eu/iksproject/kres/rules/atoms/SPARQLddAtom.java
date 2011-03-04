package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.rules.SPARQLFunction;
import eu.iksproject.kres.api.rules.SPARQLObject;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class SPARQLddAtom extends SPARQLDeleteDataAtomAbstract {

private String sparqlConstruct;
	
	public SPARQLddAtom(String sparqlConstruct) {
		this.sparqlConstruct = sparqlConstruct.substring(1, sparqlConstruct.length()-1);
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		return new SPARQLFunction(sparqlConstruct);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		return "sparql-dd(" + sparqlConstruct + ")";
	}

}
