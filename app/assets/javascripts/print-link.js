(function (window, document) {
    document.querySelectorAll('.not-supported')
        .forEach(function (element) {
            element.classList.remove('not-supported')
        })
    document.querySelectorAll('a[href="#print-dialogue"]')
        .forEach(function(link) {
            link.addEventListener('click', function(event) {
                event.preventDefault();
                window.print();
            })
        })
})(window, document)
