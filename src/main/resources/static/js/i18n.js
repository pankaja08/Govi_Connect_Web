/**
 * i18n.js - A simple Vanilla JS internationalization library
 * Handles loading JSON translations and updating the DOM
 */

const i18n = {
    currentLang: localStorage.getItem('preferredLanguage') || 'en',
    translations: {},

    /**
     * Initialize the i18n system
     */
    async init() {
        await this.loadTranslations(this.currentLang);
        this.translatePage();
        this.updateSwitcherUI();
    },

    /**
     * Load translation JSON file
     * @param {string} lang 
     */
    async loadTranslations(lang) {
        try {
            const response = await fetch(`/i18n/${lang}.json?v=${new Date().getTime()}`);
            if (!response.ok) throw new Error(`Could not load ${lang} translations`);
            this.translations = await response.json();
            this.currentLang = lang;
            localStorage.setItem('preferredLanguage', lang);
        } catch (error) {
            console.error('i18n Error:', error);
        }
    },

    /**
     * Translate all elements with data-i18n attribute
     */
    translatePage() {
        const elements = document.querySelectorAll('[data-i18n]');
        elements.forEach(el => {
            const key = el.getAttribute('data-i18n');
            const translation = this.getNestedValue(this.translations, key);
            
            if (translation) {
                // Check if it's an input/textarea for placeholder
                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    el.placeholder = translation;
                } else {
                    el.innerHTML = translation;
                }
            }
        });
        
        // Update html lang attribute
        document.documentElement.lang = this.currentLang;
    },

    /**
     * Get value from nested object using string key (e.g. 'nav.home')
     * @param {Object} obj 
     * @param {string} key 
     */
    getNestedValue(obj, key) {
        return key.split('.').reduce((prev, curr) => {
            return prev ? prev[curr] : null;
        }, obj);
    },

    /**
     * Switch language and refresh page content
     * @param {string} lang 
     */
    async switchLanguage(lang) {
        if (lang === this.currentLang) return;
        await this.loadTranslations(lang);
        this.translatePage();
        this.updateSwitcherUI();
        
        // Optional: Dispatch event for other components to listen
        window.dispatchEvent(new CustomEvent('languageChanged', { detail: lang }));
    },

    /**
     * Update the switcher text/icon in the UI
     */
    updateSwitcherUI() {
        const display = document.getElementById('current-lang-display');
        if (display) {
            const names = { 'en': 'English', 'si': 'සිංහල', 'ta': 'தமிழ்' };
            display.textContent = names[this.currentLang] || 'English';
        }
    }
};

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    i18n.init();
});

// Expose switchLanguage globally
window.switchLanguage = (lang) => i18n.switchLanguage(lang);
