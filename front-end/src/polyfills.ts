/***************************************************************************************************
 * Zone JS é necessário para o Angular funcionar corretamente.
 */
import 'zone.js'; // Incluído com o Angular CLI.

/***************************************************************************************************
 * Polyfill para internacionalização (i18n).
 * O $localize é necessário para suporte à tradução e formatação de strings.
 */
import '@angular/localize/init';

/***************************************************************************************************
 * Outros polyfills podem ser adicionados aqui, dependendo das necessidades do projeto.
 * Por exemplo, para suporte a navegadores mais antigos, você pode incluir polyfills adicionais.
 */

// Exemplo: Polyfill para suporte ao ES6+ em navegadores antigos
// import 'core-js/es/array';
// import 'core-js/es/object';
// import 'core-js/es/promise';
