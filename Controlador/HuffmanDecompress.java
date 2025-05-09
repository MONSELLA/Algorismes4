package Controlador;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import Model.Dades;
import Model.Node;

// Aquesta classe DESCOMPRIMEIX un fitxer .huff utilitzant Huffman
public class HuffmanDecompress implements Notificar {

    private final Dades dades; // Objecte amb les dades de la descompressió (arbre, etc.)

    public HuffmanDecompress(Dades dades) {
        this.dades = dades;
    }

    // DESCOMPRIMEIX el fitxer d'entrada i escriu el resultat al fitxer de sortida
    public void decompress() throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(dades.getInput().toFile())));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dades.getOutput().toFile()))) {

            // 1. Llegeix la CAPÇALERA del fitxer .huff
            int originalSize = in.readInt(); // Mida original del fitxer (en bytes)
            int symbolCount = in.readUnsignedByte(); // Nombre de símbols diferents

            boolean useShort = originalSize <= 0xFFFF; // Si el fitxer era petit, les freqüències s'han guardat amb
                                                       // short

            // Llegeix la taula de freqüències (per reconstruir l'arbre)
            Map<Byte, Integer> freq = new HashMap<>();
            for (int i = 0; i < symbolCount; i++) {
                byte symbol = in.readByte(); // Llegeix el símbol
                int f = useShort ? in.readUnsignedShort() : in.readInt(); // Llegeix la freqüència
                freq.put(symbol, f); // Desa-ho al mapa
            }

            // 2. RECONSTRUEIX l'arbre de Huffman a partir de les freqüències
            buildTree(freq);

            // 3. DECODIFICA els bits i escriu els bytes originals
            try (BitInputStream bitIn = new BitInputStream(in)) {
                int written = 0;
                while (written < originalSize) {
                    Node node = dades.getRoot(); // Comença des de l'arrel de l'arbre
                    // Recorre l'arbre fins a arribar a una fulla (un símbol)
                    while (!node.isLeaf()) {
                        boolean bit = bitIn.readBit(); // Llegeix un bit
                        node = bit ? node.getRight() : node.getLeft(); // Segueix a la dreta (1) o a l'esquerra (0)
                    }
                    out.write(node.getValue()); // Escriu el símbol recuperat
                    written++; // Comptem quants bytes hem escrit
                }
            }
        }
    }

    // Construeix l'arbre de Huffman a partir del mapa de freqüències
    private void buildTree(Map<Byte, Integer> freq) {
        PriorityQueue<Node> pq = new PriorityQueue<>(); // Cua de prioritats per construir l'arbre
        for (Map.Entry<Byte, Integer> e : freq.entrySet()) {
            pq.add(new Node(e.getKey(), e.getValue())); // Crea un node fulla per cada símbol
        }

        // Cas especial: si només hi ha un símbol, afegeix un node dummy
        if (pq.size() == 1) {
            pq.add(new Node((byte) 0, 0));
        }

        // Combina nodes fins a obtenir un únic arbre (mateix procés que al compressor)
        while (pq.size() > 1) {
            Node a = pq.poll();
            Node b = pq.poll();
            pq.add(new Node(a, b)); // Crea un nou node intern combinant dos nodes
        }
        dades.setRoot(pq.poll()); // L'últim node restant és l'arrel de l'arbre
    }

    // MAIN de prova ràpida (per comprovar que la descompressió funciona)
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Ús: java HuffmanDecompress <input.huff> <output>");
            return;
        }
        Dades dades = new Dades();
        dades.setInput(Path.of(args[0])); // Defineix el fitxer d'entrada (.huff)
        dades.setOutput(Path.of(args[1])); // Defineix el fitxer de sortida (original recuperat)
        HuffmanDecompress hd = new HuffmanDecompress(dades);
        hd.decompress(); // Fa la descompressió!
        System.out.println("Descompressió completada!");
    }

    @Override
    public void notificar(String s) {
        // Aquesta funció està preparada per enviar notificacions (per exemple, a la
        // GUI), però encara no fa res
    }
}
