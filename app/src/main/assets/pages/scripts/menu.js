document.addEventListener("DOMContentLoaded", function () {
    const currentSection = document.querySelector(".current-section");
    const dropdownMenu = document.getElementById("menu");
    const menuItems = dropdownMenu.querySelectorAll("li");

    // Открыть / закрыть меню
    function toggleDropdown() {
        dropdownMenu.classList.toggle("open");
    }

    // Делаем функцию глобальной, чтобы работал onclick
    window.toggleDropdown = toggleDropdown;

    // Обработчик выбора пункта меню
    menuItems.forEach(item => {
        item.addEventListener("click", () => {
            const sectionId = item.getAttribute("data-section");
            const target = document.getElementById(sectionId);

            if (target) {
                dropdownMenu.classList.remove("open"); // закрыть меню
                target.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });
                currentSection.textContent = item.textContent; // сменить название
            }
        });
    });

    // Закрыть при клике вне меню
    document.addEventListener("click", (e) => {
        if (!dropdownMenu.contains(e.target) && !currentSection.contains(e.target)) {
            dropdownMenu.classList.remove("open");
        }
    });
});
