/**
 * GOVI CONNECT – Hero Carousel
 * Auto-advancing 3-slide carousel with dot indicators.
 */

let currentSlide = 0;
const totalSlides = 3;
let autoPlayTimer;

function updateCarousel() {
    const slides = document.getElementById('slides');
    if (!slides) return;
    slides.style.transform = `translateX(-${currentSlide * 100}%)`;

    // Update dots
    document.querySelectorAll('.dot').forEach((dot, i) => {
        if (i === currentSlide) {
            dot.classList.add('bg-gc-gold', 'w-6');
            dot.classList.remove('bg-white/50');
        } else {
            dot.classList.remove('bg-gc-gold', 'w-6');
            dot.classList.add('bg-white/50');
        }
    });

    // Trigger enter animation on slide 1 only
    if (currentSlide === 0) {
        const hero = document.querySelector('.slide-in-up');
        if (hero) {
            hero.style.animation = 'none';
            setTimeout(() => { hero.style.animation = ''; }, 10);
        }
    }
}

function changeSlide(direction) {
    currentSlide = (currentSlide + direction + totalSlides) % totalSlides;
    updateCarousel();
    resetAutoPlay();
}

function goToSlide(index) {
    currentSlide = index;
    updateCarousel();
    resetAutoPlay();
}

function startAutoPlay() {
    autoPlayTimer = setInterval(() => changeSlide(1), 5000);
}

function resetAutoPlay() {
    clearInterval(autoPlayTimer);
    startAutoPlay();
}

// Keyboard navigation
document.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft') changeSlide(-1);
    if (e.key === 'ArrowRight') changeSlide(1);
});

// Init on page load
document.addEventListener('DOMContentLoaded', () => {
    updateCarousel();
    startAutoPlay();
});
