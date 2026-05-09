package com.smartgarden.plantmanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Traduz nomes de plantas do português para o alias científico/inglês
 * usado pela Open PlantBook API.
 *
 * <p>
 * Estratégia: mapeamento interno estático como fonte primária.
 * Se o nome já for científico ou em inglês (não encontrado no mapa),
 * é passado diretamente para a API — a Open PlantBook aceita aliases
 * científicos sem tradução.
 *
 * <p>
 * Extensão futura: substituir o Map estático por uma tabela de banco
 * de dados para permitir cadastro dinâmico de traduções via admin.
 */
@Slf4j
@Component
public class PlantNameTranslator {

  /**
   * Mapeamento PT-BR (lower-case) → alias científico para Open PlantBook.
   * Adicionar novas entradas conforme o catálogo de plantas do projeto cresce.
   */
  private static final Map<String, String> TRANSLATIONS = Map.ofEntries(
      Map.entry("samambaia", "nephrolepis exaltata"),
      Map.entry("samambaia americana", "nephrolepis exaltata"),
      Map.entry("samambaia espada", "nephrolepis exaltata"),
      Map.entry("samambaia paulista", "nephrolepis exaltata"),
      Map.entry("dinheiro em penca", "callisia repens"),

      // Orquídeas
      Map.entry("orquídea", "phalaenopsis amabilis"),
      Map.entry("orquidea", "phalaenopsis amabilis"),
      Map.entry("orquídea borboleta", "phalaenopsis amabilis"),
      Map.entry("orquidea mariposa", "phalaenopsis amabilis"),
      Map.entry("orchid", "phalaenopsis amabilis"),
      Map.entry("cattleya", "cattleya labiata"),
      Map.entry("dendrobium", "dendrobium nobile"),
      Map.entry("oncidium", "oncidium flexuosum"),
      Map.entry("chuva de ouro", "oncidium flexuosum"),
      Map.entry("vanda", "vanda coerulea"),
      Map.entry("brassavola", "brassavola perrinii"),

      // Suculentas e cactos
      Map.entry("suculenta", "echeveria"),
      Map.entry("cacto", "cactaceae"),
      Map.entry("cactus", "cactaceae"),
      Map.entry("cacto redondo", "echinocactus grusonii"),
      Map.entry("cacto bola", "echinocactus grusonii"),
      Map.entry("mandacaru", "cereus jamacaru"),
      Map.entry("xique xique", "pilosocereus gounellei"),
      Map.entry("rosa de pedra", "echeveria elegans"),
      Map.entry("planta fantasma", "graptopetalum paraguayense"),
      Map.entry("colar de pérolas", "senecio rowleyanus"),
      Map.entry("dedinho de moça", "sedum morganianum"),
      Map.entry("babosa", "aloe vera"),
      Map.entry("aloe vera", "aloe vera"),
      Map.entry("agave", "agave americana"),

      // Folhagens comuns
      Map.entry("boa-noite", "epipremnum aureum"),
      Map.entry("jiboia", "epipremnum aureum"),
      Map.entry("filodendro", "philodendron"),
      Map.entry("costela de adão", "monstera deliciosa"),
      Map.entry("monstera", "monstera deliciosa"),
      Map.entry("banana ornamental", "musa coccinea"),
      Map.entry("língua de sogra", "sansevieria trifasciata"),
      Map.entry("sansevieria", "sansevieria trifasciata"),
      Map.entry("rabo de lagarto", "sansevieria trifasciata"),
      Map.entry("pata de elefante", "beaucarnea recurvata"),
      Map.entry("dracena", "dracaena fragrans"),
      Map.entry("pleomele", "dracaena reflexa"),
      Map.entry("palmeira da felicidade", "dracaena fragrans"),
      Map.entry("lírio da paz", "spathiphyllum"),
      Map.entry("espata", "spathiphyllum"),
      Map.entry("maranta", "maranta leuconeura"),
      Map.entry("calathea", "calathea orbifolia"),
      Map.entry("ctenante", "ctenanthe setosa"),
      Map.entry("palmiteiro", "euterpe edulis"),

      // Suculentas de interior
      Map.entry("hoya", "hoya carnosa"),
      Map.entry("cacto de natal", "schlumbergera truncata"),
      Map.entry("cacto de páscoa", "schlumbergera truncata"),
      Map.entry("orelha de shrek", "crassula ovata"),
      Map.entry("jade", "crassula ovata"),
      Map.entry("planta jade", "crassula ovata"),

      // Flores e ornamentais
      Map.entry("bromelia", "bromeliaceae"),
      Map.entry("bromélia", "bromeliaceae"),
      Map.entry("antúrio", "anthurium andraeanum"),
      Map.entry("anturio", "anthurium andraeanum"),
      Map.entry("violeta africana", "saintpaulia ionantha"),
      Map.entry("violeta", "saintpaulia ionantha"),
      Map.entry("rosa", "rosa"),
      Map.entry("rosas", "rosa"),
      Map.entry("tulipa", "tulipa"),
      Map.entry("tulipas", "tulipa"),
      Map.entry("lavanda", "lavandula angustifolia"),
      Map.entry("alfazema", "lavandula angustifolia"),
      Map.entry("hortênsia", "hydrangea macrophylla"),
      Map.entry("hortensia", "hydrangea macrophylla"),
      Map.entry("azaleia", "rhododendron simsii"),
      Map.entry("azaléia", "rhododendron simsii"),
      Map.entry("gardênia", "gardenia jasminoides"),
      Map.entry("gardenia", "gardenia jasminoides"),
      Map.entry("cravo", "dianthus caryophyllus"),
      Map.entry("crisântemo", "chrysanthemum morifolium"),
      Map.entry("crisantemo", "chrysanthemum morifolium"),
      Map.entry("margarida", "leucanthemum vulgare"),
      Map.entry("girassol", "helianthus annuus"),
      Map.entry("lírio", "lilium candidum"),
      Map.entry("lirio", "lilium candidum"),
      Map.entry("íris", "iris germanica"),

      // Palmeiras
      Map.entry("palmeira", "dypsis lutescens"),
      Map.entry("palmeira rabo de raposa", "wodyetia bifurcata"),
      Map.entry("palmeira imperial", "roystonea oleracea"),
      Map.entry("palmeira jerivá", "syagrus romanzoffiana"),
      Map.entry("palmeira fênix", "phoenix canariensis"),
      Map.entry("palmeira real", "roystonea regia"),
      // Bonsai e árvores decorativas
      Map.entry("bonsai", "ficus retusa"),
      Map.entry("figueira", "ficus carica"),
      Map.entry("ficus", "ficus benjamina"),
      Map.entry("pandora", "pandanus utilis"),
      // Árvores frutíferas
      Map.entry("laranjeira", "citrus sinensis"),
      Map.entry("laranja", "citrus sinensis"),
      Map.entry("limoeiro", "citrus limon"),
      Map.entry("limão", "citrus limon"),
      Map.entry("mexeriqueira", "citrus reticulata"),
      Map.entry("tangerina", "citrus reticulata"),
      Map.entry("pessegueiro", "prunus persica"),
      Map.entry("pêssego", "prunus persica"),
      Map.entry("mangueira", "mangifera indica"),
      Map.entry("manga", "mangifera indica"),
      Map.entry("goiabeira", "psidium guajava"),
      Map.entry("goiaba", "psidium guajava"),
      Map.entry("abacateiro", "persea americana"),
      Map.entry("abacate", "persea americana"),
      Map.entry("bananeira", "musa paradisiaca"),
      Map.entry("banana", "musa paradisiaca"),
      Map.entry("lima", "citrus latifolia"),
      Map.entry("pitangueira", "eugenia uniflora"),
      Map.entry("pitanga", "eugenia uniflora"),
      Map.entry("amoreira", "morus nigra"),
      Map.entry("amora", "morus nigra"),
      Map.entry("cajueiro", "anacardium occidentale"),
      Map.entry("caju", "anacardium occidentale"),
      Map.entry("jabuticabeira", "plinia cauliflora"),
      Map.entry("jabuticaba", "plinia cauliflora"),
      Map.entry("acerola", "malpighia emarginata"),
      Map.entry("aceroleira", "malpighia emarginata"),
      Map.entry("uva", "vitis vinifera"),
      Map.entry("videira", "vitis vinifera"),
      Map.entry("cerejeira", "prunus avium"),

      // Hortaliças e temperos
      Map.entry("manjericão", "ocimum basilicum"),
      Map.entry("manjericao", "ocimum basilicum"),
      Map.entry("alecrim", "rosmarinus officinalis"),
      Map.entry("hortelã", "mentha"),
      Map.entry("hortelã pimenta", "mentha piperita"),
      Map.entry("sálvia", "salvia officinalis"),
      Map.entry("orégano", "origanum vulgare"),
      Map.entry("oregano", "origanum vulgare"),
      Map.entry("tomilho", "thymus vulgaris"),
      Map.entry("coentro", "coriandrum sativum"),
      Map.entry("salsa", "petroselinum crispum"),
      Map.entry("cebolinha", "allium fistulosum"),
      Map.entry("alho", "allium sativum"),
      Map.entry("alho poró", "allium porrum"),
      Map.entry("pimenta", "capsicum annuum"),
      Map.entry("pimentão", "capsicum annuum"),
      Map.entry("pimenta dedo de moça", "capsicum baccatum"),
      Map.entry("pimenta habanero", "capsicum chinense"),
      Map.entry("tomate", "solanum lycopersicum"),
      Map.entry("tomate cereja", "solanum lycopersicum var cerasiforme"),
      Map.entry("alface", "lactuca sativa"),
      Map.entry("rúcula", "eruca vesicaria"),
      Map.entry("rucula", "eruca vesicaria"),
      Map.entry("couve", "brassica oleracea var acephala"),
      Map.entry("espinafre", "spinacia oleracea"),
      Map.entry("brócolis", "brassica oleracea var italica"),
      Map.entry("brocolis", "brassica oleracea var italica"),
      Map.entry("couve flor", "brassica oleracea var botrytis"),
      Map.entry("abóbora", "cucurbita moschata"),
      Map.entry("moranga", "cucurbita maxima"),
      Map.entry("berinjela", "solanum melongena"),
      Map.entry("pepino", "cucumis sativus"),

      // Plantas medicinais
      Map.entry("boldo", "peumus boldus"),
      Map.entry("boldo brasileiro", "veronia condensata"),
      Map.entry("camomila", "matricaria chamomilla"),
      Map.entry("calêndula", "calendula officinalis"),
      Map.entry("calendula", "calendula officinalis"),
      Map.entry("erva cidreira", "melissa officinalis"),
      Map.entry("melissa", "melissa officinalis"),
      Map.entry("erva doce", "foeniculum vulgare"),
      Map.entry("funcho", "foeniculum vulgare"),
      Map.entry("guaco", "mikania glomerata"),
      Map.entry("carqueja", "baccharis trimera"),
      Map.entry("capim cidreira", "cymbopogon citratus"),
      Map.entry("capim limão", "cymbopogon citratus"),
      Map.entry("semente de abóbora", "cucurbita pepo"),

      // Plantas pendentes e trepadeiras
      Map.entry("hera", "hedera helix"),
      Map.entry("unha de gato", "uncaria tomentosa"),
      Map.entry("primula", "primula vulgaris"),
      Map.entry("tumbérgia", "thunbergia alata"),
      Map.entry("dama da noite", "brunfelsia uniflora"),
      Map.entry("jasmin", "jasminum officinale"),

      // Outras comuns em jardins brasileiros
      Map.entry("espada de são jorge", "sansevieria trifasciata"),
      Map.entry("espada de sao jorge", "sansevieria trifasciata"),
      Map.entry("rabo de gato", "achyranthus aspera"),
      Map.entry("tinhorão", "caladium bicolor"),
      Map.entry("singônio", "syngonium podophyllum"),
      Map.entry("pothos", "epipremnum aureum"),
      Map.entry("zamioculca", "zamioculcas zamiifolia"),
      Map.entry("zamioculcas", "zamioculcas zamiifolia"),
      Map.entry("lágrima de cristo", "coix lacryma jobi"),
      Map.entry("lagrima de cristo", "coix lacryma jobi"));

  /**
   * Traduz o nome fornecido pelo usuário para um alias compatível com a
   * Open PlantBook. Se não encontrado no mapeamento, retorna o próprio
   * nome (pode ser um nome científico ou em inglês já válido).
   *
   * @param plantName nome da planta (qualquer idioma)
   * @return alias para uso na busca da Open PlantBook
   */
  public String translate(String plantName) {
    if (plantName == null || plantName.isBlank()) {
      return plantName;
    }

    // String normalized = plantName.toLowerCase().trim();
    String normalized = plantName.toLowerCase()
        .replaceAll("[-_.]", " ")
        .replaceAll("[^a-z0-9\\s]", "")
        .replaceAll("\\s+", " ")
        .trim();

    String translated = TRANSLATIONS.getOrDefault(normalized, normalized);

    if (!translated.equals(normalized)) {
      log.debug("Plant name translated: '{}' → '{}'", plantName, translated);
    }
    return translated;
  }

  /**
   * Retorna o alias traduzido como Optional, vazio se o nome for nulo ou em
   * branco.
   */
  public Optional<String> translateOptional(String plantName) {
    if (plantName == null || plantName.isBlank())
      return Optional.empty();
    return Optional.of(translate(plantName));
  }
}
