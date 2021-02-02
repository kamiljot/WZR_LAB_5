import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import java.lang.*;


public class BookSellerAgent extends Agent
{
  // Katalog lektur na sprzedaż:
  private Hashtable catalogue;

  // Inicjalizacja klasy agenta:
  protected void setup()
  {
    // Tworzenie katalogu lektur jako tablicy rozproszonej
    catalogue = new Hashtable();

    Random randomGenerator = new Random();    // generator liczb losowych

    catalogue.put("Zamek", 90+randomGenerator.nextInt(500));       // nazwa lektury jako klucz, cena jako wartość
    catalogue.put("Opowiadania", 110+randomGenerator.nextInt(200));
    catalogue.put("Ameryka", 300+randomGenerator.nextInt(70));
    catalogue.put("Proces", 250+randomGenerator.nextInt(250));

    doWait(2019);                     // czekaj 2 sekundy

    System.out.println("Witam! Agent-sprzedawca (wersja d <2020/21>) "+getAID().getName()+" jest gotów do działania!");

    // Dodanie zachowania obsługującego odpowiedzi na oferty klientów (kupujących książki):
    addBehaviour(new OfferRequestsServer());

    // Dodanie zachowania obsługującego zamówienie klienta:
    addBehaviour(new PurchaseOrdersServer());
  }

  // Metoda realizująca zakończenie pracy agenta:
  protected void takeDown()
  {
    System.out.println("Agent-sprzedawca (wersja d <2020/21>) "+getAID().getName()+" zakończył się.");
  }


  /**
    Inner class OfferRequestsServer.
    This is the behaviour used by Book-seller agents to serve incoming requests
    for offer from buyer agents.
    If the requested book is in the local catalogue the seller agent replies
    with a PROPOSE message specifying the price. Otherwise a REFUSE message is sent back.
    */
    class OfferRequestsServer extends CyclicBehaviour
    {
      public void action()
      {
        // Tworzenie szablonu wiadomości (wstępne określenie tego, co powinna zawierać wiadomość)
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        // Próba odbioru wiadomości zgodnej z szablonem:
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {  // jeśli nadeszła wiadomość zgodna z ustalonym wcześniej szablonem
          String title = msg.getContent();  // odczytanie tytułu zamawianej książki

          System.out.println("Agent-sprzedawca "+getAID().getName()+" otrzymał wiadomość: "+
                  title);
          ACLMessage reply = msg.createReply();               // tworzenie wiadomości - odpowiedzi
          Integer price = (Integer) catalogue.get(title);     // ustalenie ceny dla podanego tytułu
          if (price != null) {                                // jeśli taki tytuł jest dostępny
            reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
            reply.setContent(String.valueOf(price.intValue()));   // umieszczenie ceny w polu zawartości (content)
            System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+
                    price.intValue());
          }
          else {                                              // jeśli tytuł niedostępny
            // The requested book is NOT available for sale.
            reply.setPerformative(ACLMessage.REFUSE);         // ustalenie typu wiadomości (odmowa)
            reply.setContent("tytuł niestety niedostępny");                  // treść wiadomości
          }
          myAgent.send(reply);                                // wysłanie odpowiedzi
        }
        else                       // jeśli wiadomość nie nadeszła, lub była niezgodna z szablonem
        {
          block();                 // blokada metody action() dopóki nie pojawi się nowa wiadomość
        }
      }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour


  class PurchaseOrdersServer extends CyclicBehaviour
  {
    int ofertaNum = 1;
    public void action()
    {
      ACLMessage msg = myAgent.receive();

      if ((msg != null)&&(msg.getPerformative() == ACLMessage.PROPOSE)) {
        String title = msg.getContent().split(";")[0];
        Double priceR = Double.parseDouble(msg.getContent().split(";")[1]);

        System.out.println("Agent-sprzedawca "+getAID().getName()+" otrzymał oferte: "+priceR);
        ACLMessage reply = msg.createReply();
        Integer price = (Integer) catalogue.get(title);
        if (price != null) {
          if(ofertaNum > 6) {
            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
            reply.setContent("");
            System.out.println("Agent-sprzedawca "+getAID().getName()+" nie negocjuje dalej.");
          } else {
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(String.valueOf(price - ofertaNum * 4));
            System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+(price - ofertaNum * 4));
            ofertaNum++;
          }
        }
        else {                                              // jeśli tytuł niedostępny
          // The requested book is NOT available for sale.
          reply.setPerformative(ACLMessage.REFUSE);         // ustalenie typu wiadomości (odmowa)
          reply.setContent("tytuł niestety niedostępny");                  // treść wiadomości
        }
        myAgent.send(reply);
      }
      else if ((msg != null)&&(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL))
      {
        // Message received. Process it
        ACLMessage reply = msg.createReply();
        String title = msg.getContent();
        reply.setPerformative(ACLMessage.INFORM);
        System.out.println("Agent-sprzedawca (wersja h 2017/18) "+getAID().getName()+" sprzedał książkę o tytule: "+title);
        myAgent.send(reply);
      }
    }
  } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour
} // Koniec klasy będącej rozszerzeniem klasy Agent