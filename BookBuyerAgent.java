import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;

// Przykładowa klasa zachowania:
class MyOwnBehaviour extends Behaviour
{
  protected MyOwnBehaviour()
  {
  }

  public void action()
  {
  }
  public boolean done() {
    return false;
  }
}


public class BookBuyerAgent extends Agent {

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private AID[] sellerAgents = {
      new AID("seller1", AID.ISLOCALNAME),
      new AID("seller2", AID.ISLOCALNAME)};
  int numOferty = 0;
    
    // Inicjalizacja klasy agenta:
    protected void setup()
    {
     
      //doWait(6000);   // Oczekiwanie na uruchomienie agentów sprzedających

      System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja d <2020/21>) jest gotów!");

      Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

      if (args != null && args.length > 0)   // jeśli podano tytuł książki
      {
        targetBookTitle = (String) args[0];
        System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

        addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań - kod znajduje się poniżej
       
      }
      else
      {
        // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
        System.out.println("Proszę podać tytuł lektury w argumentach wejściowych agenta kupującego!");
        doDelete();
      }
    }
    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
      System.out.println("kupiec "+getAID().getName()+" kończy.");
    }

    /**
    Inner class RequestPerformer.
    This is the behaviour used by Book-buyer agents to request seller
    agents the target book.
    */
    private class RequestPerformer extends Behaviour
    {
       
      private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
      private double bestPrice = 999999999;      // najlepsza cena
      private double price;
      private int repliesCnt = 0; // liczba odpowiedzi od agentów
      private MessageTemplate mt; // szablon odpowiedzi
      private int step = 0;       // krok
      private Double wantedPrice;

      public void action()
      {


        switch (step) {
        case 0:      // wysłanie oferty kupna
          System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
          for (int i = 0; i < sellerAgents.length; ++i)
          {
            System.out.print(sellerAgents[i]+ " ");
          }
          System.out.println();

          // Tworzenie wiadomości CFP do wszystkich sprzedawców:
          ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
          for (int i = 0; i < sellerAgents.length; ++i)
          {
            cfp.addReceiver(sellerAgents[i]);                // dodanie adresata
          }
          cfp.setContent(targetBookTitle);                   // wpisanie zawartości - tytułu książki
          cfp.setConversationId("handel_ksiazkami");         // wpisanie specjalnego identyfikatora korespondencji
          cfp.setReplyWith("cfp"+System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
          myAgent.send(cfp);                           // wysłanie wiadomości

          // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
          mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                   MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
          step = 1;     // przejście do kolejnego kroku
          break;
        case 1:      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
          ACLMessage reply = myAgent.receive(mt);      // odbiór odpowiedzi

          if (reply != null)
          {
            if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
            {
              price = Integer.parseInt(reply.getContent());  // cena książki
              if (price < bestPrice)       // jeśli jest to najlepsza oferta
              {
                bestPrice = price;
                bestSeller = reply.getSender();
                wantedPrice = bestPrice * 0.5;
              }
            }
            repliesCnt++;                                        // liczba ofert

            if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
            {
              step = 2;
            }
          }
          else
          {
            block();
          }
          break;
        case 2:      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
          if(numOferty >= 6){
            System.out.println("Kupiec odrzucil oferte kupna "+targetBookTitle+".");
            myAgent.doDelete();
            step = 9;
            break;
          }
          System.out.println(numOferty);
          System.out.println("Kupiec proponuje "+ wantedPrice +" za "+targetBookTitle+".");
          ACLMessage order = new ACLMessage(ACLMessage.PROPOSE);
          order.addReceiver(bestSeller);
          order.setContent(targetBookTitle + ";" + wantedPrice.toString());
          order.setConversationId("book-trade");
          order.setReplyWith("neg"+System.currentTimeMillis());
          System.out.println(order.toString());
          myAgent.send(order);
          mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                  MessageTemplate.MatchInReplyTo(order.getReplyWith()));
          step = 3;
          break;

        case 3:
          reply = myAgent.receive(mt);

        if (reply == null){
          break;
        }
          /*System.out.println("jesteśmy w case 3");*/
        bestPrice = Double.parseDouble(reply.getContent());
        if((wantedPrice - 2) <= bestPrice && (wantedPrice + 2) >= bestPrice) {
          step = 4;
          break;
        } else{
          numOferty++;
          System.out.println(numOferty);
          wantedPrice = wantedPrice + 5;
          step = 2;
          break;

        }

          case 4:      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
            order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            order.addReceiver(bestSeller);
            order.setContent(targetBookTitle);
            order.setConversationId("handel_ksiazkami");
            order.setReplyWith("order"+System.currentTimeMillis());
            System.out.println("O, kupuję za "+bestPrice+".");
            myAgent.send(order);
            System.out.println("Zamówione.");
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
            step = 5;
            break;

          case 5:      // odbiór odpowiedzi na zamównienie
            reply = myAgent.receive(mt);
            if (reply != null)
            {
              if (reply.getPerformative() == ACLMessage.INFORM)
              {
                System.out.println("Tytuł "+targetBookTitle+" zamówiony!");
                System.out.println("Po cenie: "+bestPrice);
                myAgent.doDelete();
              }
              step = 6;
            }
            else
            {
              block();
            }
            break;
        }  // switch
      } // action

      public boolean done() {
        return ((step == 2 && bestSeller == null) || step == 9);
      }
    } // Koniec wewnętrznej klasy RequestPerformer
}
