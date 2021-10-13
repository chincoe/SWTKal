/*****************************************************************************************************
 * 	Project:			SWTKal.Base
 *
 *  creation date:		01.08.2007
 *
 *
 *****************************************************************************************************
 *	date			| 	author		| 	reason for change
 *****************************************************************************************************
 *	01.08.2007		swtUser			initial version
 *
 */
package swtkal.server;

import java.util.*;
import java.util.stream.Collectors;

import swtkal.domain.Datum;
import swtkal.domain.Person;
import swtkal.domain.Termin;
import swtkal.exceptions.PersonException;
import swtkal.exceptions.TerminException;

/*****************************************************************************************************
 * Class SimpleServer is a single-user, memory-based server that can be
 * used to easily test the SWTKal application (especially its graphical
 * user interfaces for client and monitor!).
 *
 * This simplistic implementation intensively uses Java generic collection
 * classes to realize server functionality.
 *
 * The server is initialized with a user Admin with kuerzel "ADM" and password "admin".
 * Furthermore there are two appointments for the current date.
 *
 * @author swtUser
 */
public class SimpleServer extends Server {
    protected Map<String, Person> personen;
    protected Map<String, String> passwoerter;
    // Diese map beinhaltet alle Termine mit ihrer ID. Für alle anderen Relationen wird nur die ID des Termins referenziert.
    protected Map<Integer, Termin> termine;
    protected Map<String, Vector<Integer>> tnTermine;
    protected Map<String, Vector<Integer>> dateTermine;

//  TODO analoge Datenstruktur und Interface-Methoden fuer Besitzer-Assoziation einfuegen?	
//	protected Map<String, Map<String, Vector<Termin>>> besitzerTermine;

    /**
     * This constructor creates an initial default user and two appointments
     */
    protected SimpleServer() {
        personen = new HashMap<String, Person>();
        passwoerter = new HashMap<String, String>();
        termine = new HashMap<>();
        tnTermine = new HashMap<>();
        dateTermine = new HashMap<>();
        try {
            // administrator as initial default user
            Person p = new Person("SWTKal", "Admin", "ADM");
            insert(p, "admin");
            //	two simple test dates for today
            insert(new Termin(p, "1. Testtermin", "Dies ist der Langtext zum 1. Testtermin",
                    new Datum(new Date()), new Datum(new Date()).addDauer(1)));
            insert(new Termin(p, "2. Testtermin", "Dies ist der Langtext zum 2. Testtermin",
                    new Datum(new Date()).addDauer(1.5), new Datum(new Date()).addDauer(2.5)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insert(Person p, String passwort) throws PersonException {
        logger.fine("Insertion of person " + p + " with a password");

        String kuerzel = p.getKuerzel();

        if (isPersonKnown(kuerzel))
            throw new PersonException("Userid is already used!");
        passwoerter.put(kuerzel, passwort);
        personen.put(kuerzel, p);
    }

    public void delete(Person p) throws PersonException {
        logger.fine("Deletion of person " + p);

        String kuerzel = p.getKuerzel();
        if (!isPersonKnown(kuerzel))
            throw new PersonException("Userid unknown!");
        tnTermine.remove(kuerzel);
        personen.remove(kuerzel);
        passwoerter.remove(kuerzel);
    }

    public void update(Person p) throws PersonException {
        logger.fine("Update of person " + p);

        String kuerzel = p.getKuerzel();
        if (!isPersonKnown(kuerzel))
            throw new PersonException("Userid unknown!");
        personen.put(kuerzel, p);
    }

    public void updatePasswort(Person p, String passwort) throws PersonException {
        logger.fine("Update of password of person " + p);

        String kuerzel = p.getKuerzel();
        if (!isPersonKnown(kuerzel))
            throw new PersonException("Userid unknown!");
        passwoerter.put(kuerzel, passwort);
    }

    public void updateKuerzel(Person p, String oldKuerzel) throws PersonException {
        logger.fine("Update of userid of person " + p);

        String neuKuerzel = p.getKuerzel();
        if (neuKuerzel.equals(oldKuerzel)) return;        // nothing to do
        if (!isPersonKnown(oldKuerzel))
            throw new PersonException("Userid unknown!");
        if (isPersonKnown(neuKuerzel))
            throw new PersonException("Userid is already used!");

        personen.remove(oldKuerzel);
        personen.put(neuKuerzel, p);

        passwoerter.put(neuKuerzel, passwoerter.get(oldKuerzel));
        passwoerter.remove(oldKuerzel);
    }

    public Person authenticatePerson(String kuerzel, String passwort)
            throws PersonException {
        logger.fine("Authentication of userid " + kuerzel + " with a password");

        if (!isPersonKnown(kuerzel)) {
            logger.warning("Failed authentication for userid " + kuerzel);
            throw new PersonException("Userid unknown!");
        }
        Person p = personen.get(kuerzel);
        if (passwort.equals(passwoerter.get(kuerzel)))
            return p;
        else {
            logger.warning("Wrong password for userid " + kuerzel);
            throw new PersonException("Wrong password!");
        }
    }

    public boolean isPersonKnown(String kuerzel) {
        return passwoerter.containsKey(kuerzel);
    }

    public Person findPerson(String kuerzel) throws PersonException {
        logger.fine("Find person with userid " + kuerzel);

        if (!isPersonKnown(kuerzel))
            throw new PersonException("Userid unknown!");
        return personen.get(kuerzel);
    }

    public Vector<Person> getPersonVector() {
        logger.fine("Method getPersonVector called");

        return new Vector<Person>(personen.values());
    }

    public void insert(Termin termin) throws TerminException {
        logger.fine("Insertion of date " + termin);
        if (termin.getId() == 0) {
            termin.setId(termine.keySet().stream().max(Integer::compareTo).orElse(1));
        }

        // add termin to respective date
        var dateTermin = dateTermine.get(termin.getBeginn().getDateStr());
        if (!dateTermin.contains(termin.getId())) {
            dateTermin.add(termin.getId());
        }

        // add termin to all relevant teilnehmer
        for (Person p : termin.getTeilnehmer()) {
            if (!isPersonKnown(p.getKuerzel()))
                throw new TerminException("Userid unknown!");
            var tnTermin = tnTermine.get(p.getKuerzel());
            if (!tnTermin.contains(termin.getId())) {
                tnTermin.add(termin.getId());
            }
        }

        // add termin to list
        termine.put(termin.getId(), termin);

//		// insert into besitzerTermine
//		String kuerzel = termin.getBesitzer().getKuerzel();
//		if (!isPersonKnown(kuerzel))
//			throw new TerminException("Userid unknown!");
//		insert(termin, termin.getBesitzer(), besitzerTermine);
    }

    public void delete(Termin termin) throws TerminException {
        logger.fine("Deletion of date " + termin);

        var terminId = termin.getId();
        // remove termin from tnTermine
        delete(terminId);

//		// delete from besitzerTermine
//		String kuerzel = termin.getBesitzer().getKuerzel();
//		if (!personen.containsKey(kuerzel))
//			throw new TerminException("Userid unknown!");
//		delete(termin, termin.getBesitzer(), besitzerTermine);
    }

    private void delete(Termin termin, Person p, Map<String, Map<String, Vector<Termin>>> map) {
        Map<String, Vector<Termin>> dayMap = map.get(p.getKuerzel());
        if (dayMap != null) {
            Vector<Termin> vector = dayMap.get(termin.getBeginn().getDateStr());
            if (vector != null)
                vector.remove(termin);
        }
    }

    public void update(Termin termin) throws TerminException {
        // TODO Auto-generated method stub
        throw new TerminException("Not yet implemented!");
    }

    public Termin getTermin(int terminId) throws TerminException {
        if (termine.containsKey(terminId)) {
            return termine.get(terminId);
        }

        throw new TerminException("Termin does not exist");
    }

    public void delete(int terminId) throws TerminException {
        try {
            var existingTermin = getTermin(terminId);
            tnTermine.forEach((tnKey, tnTerminList) -> {
                tnTerminList.removeIf(tId -> tId == terminId);
                if (tnTerminList.isEmpty()) {
                    tnTermine.remove(tnKey);
                }
            });
            // remove termin from date termine
            var dateKey = existingTermin.getBeginn().getDateStr();
            var dateTerminList = dateTermine.get(dateKey);
            dateTerminList.removeIf(tId -> tId == terminId);
            if (dateTerminList.isEmpty()) {
                dateTermine.remove(dateKey);
            }
            // remove termin
            termine.remove(terminId);
        } catch (TerminException tex) {
            // ignored
        }
    }

    public Vector<Termin> getTermineVom(Datum dat, Person tn)
            throws TerminException {
        logger.fine("Method getTermineVom called for " + dat);

        String kuerzel = tn.getKuerzel();
        if (!isPersonKnown(kuerzel))
            throw new TerminException("Userid unknown!");

        // get all termine that are specific to the user and the date
        var tnTerminIds = tnTermine.get(kuerzel);
        var dateTerminIds = dateTermine.get(dat.getDateStr());
        return dateTerminIds
                .stream()
                .filter(dtId -> !tnTerminIds.contains(dtId))
                .map(dtId -> termine.get(dtId))
                .collect(Collectors.toCollection(Vector::new));
    }

    public Vector<Termin> getTermineVonBis(Datum vonDat, Datum bisDat, Person tn)
            throws TerminException {
        logger.fine("Method getTermineVonBis called from " + vonDat + " to " + bisDat);

        String kuerzel = tn.getKuerzel();
        if (!isPersonKnown(kuerzel))
            throw new TerminException("Userid unknown!");
        if (vonDat.isGreater(bisDat) == 1)
            throw new TerminException("Incorrect date interval!");

        // get all terminIds for the respective user
        var tnTerminIds = tnTermine.get(kuerzel);

        // get all terminIds for the respective dates
        var dateTerminIds = new Vector<Integer>();
        var d = new Datum(vonDat);
        while (bisDat.isGreater(d) == 1) {
            var moreDateTerminIds = dateTermine.get(d.getDateStr());
            if (moreDateTerminIds != null) dateTerminIds.addAll(moreDateTerminIds);
            d.add(1);    // next day
        }

        // get all termine for the user at the dates
        return dateTerminIds
                .stream()
                .filter(dtId -> !tnTerminIds.contains(dtId))
                .map(dtId -> termine.get(dtId))
                .collect(Collectors.toCollection(Vector::new));
    }

    public Vector<Termin> getTermineVom(Datum dat, Vector<Person> teilnehmer)
            throws TerminException {
        throw new TerminException("Not yet implemented!");
        // TODO Auto-generated method stub
    }

    public Vector<Termin> getTermineVonBis(Datum vonDat, Datum bisDat, Vector<Person> teilnehmer)
            throws TerminException {
        // TODO Auto-generated method stub
        throw new TerminException("Not yet implemented!");
    }

    public Vector<Termin> getBesitzerTermineVom(Datum dat, Person besitzer)
            throws TerminException {
        // TODO Auto-generated method stub
        throw new TerminException("Not yet implemented!");
    }

    public Vector<Termin> getBesitzerTermineVonBis(Datum vonDat, Datum bisDat, Person besitzer)
            throws TerminException {
        // TODO Auto-generated method stub
        throw new TerminException("Not yet implemented!");
    }

    public boolean isPersonAvailable(Datum vonDat, Datum bisDat, Person teilnehmer)
            throws TerminException {
        // TODO Auto-generated method stub
        throw new TerminException("Not yet implemented!");
    }

}
