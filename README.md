# Player de áudio "TolaFy"

Este projeto Java implementa um player de áudio com funcionalidades aprimoradas. As principais melhorias incluem a adição de músicas à lista de reprodução, a remoção de músicas, a reprodução aleatória (shuffle) e a reprodução em loop, tornando a experiência do usuário mais eficiente e amigável.

# Funcionalidades Aprimoradas
## Adicionar e Remover Músicas
* Adicionar Músicas: Ao pressionar o botão "Adicionar Música", uma nova janela permite que o usuário escolha um arquivo de música para adicionar à lista de reprodução. A adição de músicas não interrompe a reprodução em curso.
*Remover Músicas: O botão "Remover" permite a exclusão da música selecionada na lista de reprodução. Se a música estiver em reprodução, a reprodução será interrompida imediatamente.

## Reproduzir Músicas
* Reproduzir Músicas: Ao pressionar "Play Now", a música selecionada é reproduzida do início ao fim, com informações exibidas no "miniplayer". A reprodução continua em sequência até o final da lista de reprodução.
* Pausar e Continuar (Play/Pause): O botão "Play/Pause" possui três estados: desabilitado com ícone "Play" (quando não há reprodução em andamento ou após pressionar "Stop"), habilitado com ícone "Pause" (quando a reprodução está em andamento) e habilitado com ícone "Play" (quando a reprodução está pausada).
* Parar a Reprodução (Stop): O botão "Stop" fica habilitado apenas durante a reprodução ou pausa. Pressioná-lo interrompe imediatamente a reprodução e retorna o "miniplayer" ao estado padrão.

## Manipulação de Vetores
### Reprodução Aleatória (Shuffle):
* O botão "Shuffle" fica desabilitado quando não há músicas na lista de reprodução.
No estado "stopped", pressionar "Shuffle" reorganiza a ordem da lista de reprodução de forma aleatória. A reprodução segue na nova ordem.
* Se alguma música estiver sendo reproduzida, pressionar "Shuffle" move a música atual para o início da lista e reorganiza o restante aleatoriamente.
* Pressionar novamente "Shuffle" restaura a ordem original.
### Reprodução em Loop:
* O botão "Loop" fica desabilitado quando não há músicas na lista de reprodução.
* No modo loop, a reprodução da lista recomeça ao chegar ao fim.
* A reprodução da música atual não é interrompida ao pressionar "Loop".
* Se "Shuffle" e "Loop" estiverem pressionados, a ordem aleatória atual continua no próximo loop.

## Outras Funcionalidades (Features)
* Adição de músicas no modo shuffle coloca-as no final da fila.
* Remoção de músicas no modo shuffle as remove da lista de reprodução, independentemente do modo anterior.
* O scrubber reflete o progresso da música durante a reprodução e pausa.
* A mudança no progresso ao clicar ou arrastar o scrubber reflete na reprodução, permitindo que a música continue do quadro selecionado.


Este player de áudio aprimorado oferece uma experiência mais completa e flexível, permitindo que os usuários desfrutem de suas músicas com facilidade e controle. Sinta-se à vontade para utilizar e contribuir com melhorias, seguindo a licença MIT fornecida no projeto.
