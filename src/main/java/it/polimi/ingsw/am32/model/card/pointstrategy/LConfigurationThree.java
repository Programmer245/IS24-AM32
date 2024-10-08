package it.polimi.ingsw.am32.model.card.pointstrategy;

import it.polimi.ingsw.am32.model.field.CardPlaced;
import it.polimi.ingsw.am32.model.field.Field;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * LConfigurationThree is one of the classes that implement the PointStrategy interface used to calculate the
 * objective cards, which in the top left has an Animal card and in the bottom right corner has an Insect card
 * and then under that card has another Insect card.
 *
 * @author Jie
 */
public class LConfigurationThree implements PointStrategy {
    /**
     * Calculate how many times the L configuration is fulfilled in the player's field, where in the top left is an
     * Animal card, and in the right down found two Insect cards.
     * @param field Field of play where the card placed.
     * @param x The x coordinate of the card whose points are being calculated.
     * @param y The y coordinates of the card whose points are being calculated.
     * @return Number of times that objective card has been fulfilled in this field.
     */
    public int calculateOccurrences(Field field, int x, int y) {
        int times = 0;
        List<CardPlaced> animalKingdom = new ArrayList<>(); // create Arraylist to store all cards of animal Kingdom visible in field.
        for (CardPlaced i : field.getFieldCards()) {
            if (i.getNonObjectiveCard().getKingdom() == ObjectType.ANIMAL) {
                animalKingdom.add(i);
            }
        } // add a card to the animalKingdom list if its type is Animal.
        List<CardPlaced> insectKingdom = new ArrayList<>(); // create Arraylist to store all cards of Insect Kingdom visible in field.
        for (CardPlaced i : field.getFieldCards()) {
            if (i.getNonObjectiveCard().getKingdom() == ObjectType.INSECT) {
                insectKingdom.add(i);
            }
        } // add a card to the insertKingdom list if its type is Insect.
        Comparator<CardPlaced> comparatorTwo = (c1, c2) -> {
            if (c1.getX() > c2.getX()) {
                return 1;
            } else if (c1.getX() < c2.getX()) {
                return -1;
            } else
                return Integer.compare(c1.getY(), c2.getY());
        };

        animalKingdom.sort(comparatorTwo);
        insectKingdom.sort(comparatorTwo);

        while (!animalKingdom.isEmpty()) {
            int xLoc;
            int yLoc;
            xLoc = animalKingdom.getFirst().getX();
            yLoc = animalKingdom.getFirst().getY();
            CardPlaced e1;
            for(int j=0; j<insectKingdom.size()&&insectKingdom.get(j).getX()<(xLoc+2); j++){ // search for Insect cards that connect to the specific Animal card.
                if (insectKingdom.get(j).getX() == (xLoc + 1) && insectKingdom.get(j).getY() == (yLoc - 1)) {
                    e1 = insectKingdom.get(j); // an Insect card found in the top left corner of the specific Animal card.
                    if(j!=0){
                    if (insectKingdom.get(j - 1).getX() == e1.getX() && insectKingdom.get(j - 1).getY() == e1.getY() - 2) {
                        insectKingdom.remove(insectKingdom.get(j - 1));
                        times++;
                        insectKingdom.remove(e1);
                    }
                    }

                }
            } // remove elements validated to reduce the search time.
            animalKingdom.remove(animalKingdom.getFirst());
        }
        insectKingdom.clear();
        return times;
    }
}
