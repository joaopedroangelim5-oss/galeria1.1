# Gallery Kisser

Galeria privada para Android. Diferente de uma galeria comum, ela **não lê
automaticamente as fotos do aparelho** — você importa manualmente, e a partir
daí o app guarda a própria cópia isolada. Apagar o arquivo original fora do
app não afeta a cópia dentro do Gallery Kisser, e as fotos importadas **não
aparecem** no Google Fotos, no app de Arquivos, nem em outros apps.

## Funcionalidades

- Importação manual de fotos e vídeos (seletor do sistema, sem precisar de
  permissão de armazenamento em runtime)
- Armazenamento 100% privado, isolado dentro da pasta de dados do app
  (`filesDir/gk_media/`)
- Feed principal com as mídias organizadas por data ("Hoje", "Ontem", depois
  por dia), em grade
- Criação de pastas para organizar as mídias
- Plano de fundo customizável para a área vazia da galeria (configurável)
- Bloqueio por PIN numérico (4 dígitos), com hash — o PIN nunca fica salvo em
  texto puro
- Menu lateral (drawer) com as pastas e as configurações
- Seleção múltipla: mover para pasta, excluir do app, ou salvar de volta no
  aparelho (exportar) — exportação funciona **sem internet**, é cópia local
- Visualizador em tela cheia para fotos e vídeos, com navegação entre itens

## Como gerar o APK sem usar o Android Studio (GitHub Actions)

O projeto já vem com uma receita de build automática em
`.github/workflows/build-apk.yml`. Assim que você subir o código pro GitHub:

1. Vá na aba **Actions** do seu repositório.
2. Vai aparecer um workflow chamado **"Build APK"** rodando (ou clique em
   "Run workflow" pra disparar manualmente).
3. Espere terminar (ícone verde ✅ — leva alguns minutos).
4. Clique no workflow concluído → na seção **Artifacts**, baixe
   `gallery-kisser-debug-apk`. Dentro está o `app-debug.apk`, pronto pra
   instalar no celular (ative "Instalar de fontes desconhecidas" no Android
   se for a primeira vez).

Esse APK gerado é de **debug** (não assinado para distribuição na Play
Store, mas instala e funciona normalmente em qualquer aparelho Android).

## Como abrir e gerar o APK pelo Android Studio (alternativa)

1. Abra a pasta `GalleryKisser` no **Android Studio** (versão recente, com o
   Android SDK 34 instalado).
2. Deixe o Gradle sincronizar (ele vai baixar as dependências automaticamente
   — Room, Glide, Material Components etc.).
3. Para gerar o APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.
4. Para gerar uma versão assinada (release, pronta pra distribuir):
   `Build > Generate Signed Bundle / APK`, escolha APK, crie ou selecione seu
   keystore, e siga o assistente.

> Não incluí nenhuma keystore de assinatura — cada dev deve gerar a sua
> (`Build > Generate Signed Bundle / APK > Create new...`). Isso é proposital,
> por segurança: uma keystore nunca deve ir para o repositório Git.

## Estrutura do projeto

```
app/src/main/java/com/gallerykisser/app/
├── data/     -> Room (banco), armazenamento privado de arquivos, repositório
├── ui/       -> Activities, ViewModel, Adapter da grade
└── util/     -> Gerenciador de PIN e de plano de fundo (SharedPreferences)
```

## Ícone

O ícone enviado já está aplicado em todas as densidades
(`mipmap-mdpi` a `mipmap-xxxhdpi`). Se quiser um ícone adaptativo (formato que
se molda ao contorno do launcher em Android 8+), use o **Image Asset Studio**
do Android Studio (botão direito em `res` > `New > Image Asset`) apontando
para a imagem original — ele gera as camadas adaptativas automaticamente.

## Publicando no GitHub

Depois de testar o build localmente:

```bash
cd GalleryKisser
git init
git add .
git commit -m "Gallery Kisser - versão inicial"
git branch -M main
git remote add origin <url-do-seu-repositorio>
git push -u origin main
```

Depois de subir, o APK sai sozinho na aba **Actions** (veja a seção acima) —
não precisa fazer mais nada manualmente.
