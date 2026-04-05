document.addEventListener('DOMContentLoaded', function () {
    const dobInput = document.getElementById('dobInput');
    const regForm = document.getElementById('regForm');

    // Input elements
    const fullName = document.querySelector('input[name="fullName"]');
    const nic = document.querySelector('input[name="nic"]');
    const contactNumber = document.querySelector('input[name="contactNumber"]');
    const email = document.querySelector('input[name="email"]');
    const username = document.querySelector('input[name="username"]');
    const password = document.querySelector('input[name="password"]');

    // Officer elements
    const agriOfficerToggle = document.getElementById('agriOfficerToggle');
    const regNum = document.getElementById('regNum');
    const designation = document.getElementById('designation');
    const officialEmail = document.getElementById('officialEmail');

    // Error span elements
    const errors = {
        fullName: document.getElementById('fullNameError'),
        nic: document.getElementById('nicError'),
        dob: document.getElementById('dobError'),
        contact: document.getElementById('contactError'),
        email: document.getElementById('emailError'),
        username: document.getElementById('usernameError'),
        password: document.getElementById('passwordError'),
        regNum: document.getElementById('regNumError'),
        designation: document.getElementById('designationError'),
        officialEmail: document.getElementById('officialEmailError'),
        district: document.getElementById('districtError'),
        province: document.getElementById('provinceError'),
        address: document.getElementById('addressError')
    };

    // Helper to show/hide errors
    function setValidation(element, errorSpan, isValid, message) {
        if (!element || !errorSpan) return;

        // NEW: Also find any sibling server-side error spans (Thymeleaf th:errors)
        const parent = element.parentElement;
        const serverErrors = parent ? parent.querySelectorAll('.text-red-500') : [];

        if (isValid) {
            errorSpan.textContent = '';
            errorSpan.classList.add('hidden');
            element.classList.remove('border-red-500');
            element.classList.add('border-green-500', 'border-2');
            
            // Hide all error messages in this container
            serverErrors.forEach(err => {
                err.classList.add('hidden');
                err.textContent = ''; // Clear text to be safe
            });
        } else {
            errorSpan.textContent = message;
            errorSpan.classList.remove('hidden');
            element.classList.remove('border-green-500', 'border-2');
            element.classList.add('border-red-500', 'border-2');
        }
    }

    // Validation Functions
    function validateFullName() {
        const val = fullName.value.trim();
        if (val === '') {
            setValidation(fullName, errors.fullName, false, "Full name is required.");
            return false;
        }
        setValidation(fullName, errors.fullName, true, "");
        return true;
    }

    function validateNic() {
        const val = nic.value.trim();
        if (val === '') {
            setValidation(nic, errors.nic, false, "NIC number is required.");
            return false;
        }
        const regex = /^([0-9]{9}[a-zA-Z]|[0-9]{12})$/;
        const isValid = regex.test(val);
        setValidation(nic, errors.nic, isValid, "Enter a valid NIC (12 digits or 9 digits + 1 letter).");
        return isValid;
    }

    function validateDob() {
        const val = dobInput.value;
        if (val === '') {
            setValidation(dobInput, errors.dob, false, "Date of birth is required.");
            return false;
        }
        const selectedDate = new Date(val);
        const today = new Date();
        const sixteenYearsAgo = new Date(today.getFullYear() - 16, today.getMonth(), today.getDate());
        const isValid = selectedDate <= sixteenYearsAgo;
        setValidation(dobInput, errors.dob, isValid, "Your age must be greater than 16 years old.");
        return isValid;
    }

    function validateContact() {
        if (!contactNumber) return true;
        const val = contactNumber.value.trim();
        if (val === '') {
            setValidation(contactNumber, errors.contact, false, "Contact number is required.");
            return false;
        }
        const regex = /^0\d{9}$/;
        const isValid = regex.test(val);
        setValidation(contactNumber, errors.contact, isValid, "Enter valid contact number (10 digits starting with 0).");
        return isValid;
    }

    function validateEmail() {
        const val = email.value.trim();
        if (val === '') {
            setValidation(email, errors.email, false, "Email address is required.");
            return false;
        }
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const isValid = regex.test(val);
        setValidation(email, errors.email, isValid, "Enter a valid email address.");
        return isValid;
    }

    function validateUsername() {
        const val = username.value.trim();
        if (val === '') {
            setValidation(username, errors.username, false, "Username is required.");
            return false;
        }
        const isValid = val.length >= 4 && val.length <= 30;
        setValidation(username, errors.username, isValid, "Username must be between 4 and 30 characters.");
        return isValid;
    }

    function validatePassword() {
        const val = password.value;
        if (val === '') {
            setValidation(password, errors.password, false, "Password is required.");
            return false;
        }
        const isValid = val.length >= 8;
        setValidation(password, errors.password, isValid, "Password must be at least 8 characters long.");
        return isValid;
    }

    function validateLocation() {
        const district = document.getElementById('district');
        const province = document.getElementById('province');
        const address = document.getElementById('address');
        
        let ok = true;
        if (district && district.value === '') {
            setValidation(district, errors.district, false, "District is required.");
            ok = false;
        } else if (district) {
            setValidation(district, errors.district, true, "");
        }
        
        if (province && province.value === '') {
            setValidation(province, errors.province, false, "Province is required.");
            ok = false;
        } else if (province) {
            setValidation(province, errors.province, true, "");
        }

        if (address && address.value.trim() === '') {
            setValidation(address, errors.address, false, "Address is required.");
            ok = false;
        } else if (address) {
            setValidation(address, errors.address, true, "");
        }

        return ok;
    }

    function validateOfficerFields() {
        if (!agriOfficerToggle || !agriOfficerToggle.checked) return true;

        let ok = true;
        
        // Reg Num
        const rn = regNum.value.trim();
        if (rn === '') {
            setValidation(regNum, errors.regNum, false, "Registration number is required.");
            ok = false;
        } else {
            setValidation(regNum, errors.regNum, true, "");
        }

        // Designation
        const ds = designation.value.trim();
        if (ds === '') {
            setValidation(designation, errors.designation, false, "Designation is required.");
            ok = false;
        } else {
            setValidation(designation, errors.designation, true, "");
        }

        // Official Email
        const oe = officialEmail.value.trim();
        if (oe === '') {
            setValidation(officialEmail, errors.officialEmail, false, "Official email is required.");
            ok = false;
        } else {
            const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            const isValid = regex.test(oe);
            setValidation(officialEmail, errors.officialEmail, isValid, "Valid official email is required.");
            if (!isValid) ok = false;
        }

        return ok;
    }

    // Attach Listeners
    if (fullName) fullName.addEventListener('input', validateFullName);
    if (nic) nic.addEventListener('input', validateNic);
    if (dobInput) dobInput.addEventListener('input', validateDob);
    if (contactNumber) contactNumber.addEventListener('input', validateContact);
    if (email) email.addEventListener('input', validateEmail);
    if (username) username.addEventListener('input', validateUsername);
    if (password) password.addEventListener('input', validatePassword);
    
    if (regNum) regNum.addEventListener('input', validateOfficerFields);
    if (designation) designation.addEventListener('input', validateOfficerFields);
    if (officialEmail) officialEmail.addEventListener('input', validateOfficerFields);

    // Location Listeners
    const dist = document.getElementById('district');
    const prov = document.getElementById('province');
    const addr = document.getElementById('address');
    if (dist) dist.addEventListener('change', validateLocation);
    if (prov) prov.addEventListener('change', validateLocation);
    if (addr) addr.addEventListener('input', validateLocation);

    // DOB constraint initialization
    if (dobInput) {
        const today = new Date();
        const sixteenYearsAgo = new Date(today.getFullYear() - 16, today.getMonth(), today.getDate());
        const maxDate = sixteenYearsAgo.toISOString().split('T')[0];
        dobInput.setAttribute('max', maxDate);
    }

    // Prevent form submission if JS validation fails
    if (regForm) {
        regForm.addEventListener('submit', function (e) {
            const isFullNameOk = validateFullName();
            const isNicOk = validateNic();
            const isDobOk = validateDob();
            const isContactOk = validateContact();
            const isEmailOk = validateEmail();
            const isUsernameOk = validateUsername();
            const isPasswordOk = validatePassword();
            const isLocationOk = validateLocation();
            const isOfficerOk = validateOfficerFields();

            const hasErrors = !isFullNameOk || !isNicOk || !isDobOk || !isContactOk || !isEmailOk || !isUsernameOk || !isPasswordOk || !isLocationOk || !isOfficerOk;

            if (hasErrors) {
                e.preventDefault();
                // Scroll to the first error element for better UX
                const firstError = document.querySelector('.border-red-500');
                if (firstError) {
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        });
    }
});
