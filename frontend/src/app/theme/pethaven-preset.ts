import { definePreset } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

export const PetHavenPreset = definePreset(Aura, {
  primitive: {
    borderRadius: {
      none: '0',
      xs: '6px',
      sm: '8px',
      md: '10px',
      lg: '12px',
      xl: '20px',
    },
    // success family (sage)
    green: {
      50: '#fafbf9',
      100: '#e0e7da',
      200: '#c4d1b9',
      300: '#b1c3a2',
      400: '#a4b993',
      500: '#9db38a',
      600: '#86a16e',
      700: '#6e8758',
      800: '#566944',
      900: '#3e4c31',
      950: '#262e1e',
    },
    // warn family (honey) — button/tag severity tokens use `orange`
    orange: {
      50: '#fefbf6',
      100: '#f8e6c6',
      200: '#f1d093',
      300: '#edc171',
      400: '#eab659',
      500: '#e8b04b',
      600: '#e39f24',
      700: '#bf8418',
      800: '#966713',
      900: '#6c4a0e',
      950: '#422e08',
    },
    // warn family (honey) — message/toast severity tokens use `yellow`
    yellow: {
      50: '#fefbf6',
      100: '#f8e6c6',
      200: '#f1d093',
      300: '#edc171',
      400: '#eab659',
      500: '#e8b04b',
      600: '#e39f24',
      700: '#bf8418',
      800: '#966713',
      900: '#6c4a0e',
      950: '#422e08',
    },
    // danger family (terracotta)
    red: {
      50: '#fdf9f7',
      100: '#eed2c7',
      200: '#dfa993',
      300: '#d48d70',
      400: '#cd7a58',
      500: '#c96f4a',
      600: '#b65b36',
      700: '#964b2d',
      800: '#763b23',
      900: '#562b1a',
      950: '#361b10',
    },
    // info family (muted teal) — button/tag severity tokens use `sky`
    sky: {
      50: '#f9fafb',
      100: '#cddbdf',
      200: '#9eb8c1',
      300: '#7fa1ad',
      400: '#69919f',
      500: '#5f8794',
      600: '#52747f',
      700: '#446069',
      800: '#354c53',
      900: '#27383d',
      950: '#192427',
    },
    // info family (muted teal) — message/toast severity tokens use `blue`
    blue: {
      50: '#f9fafb',
      100: '#cddbdf',
      200: '#9eb8c1',
      300: '#7fa1ad',
      400: '#69919f',
      500: '#5f8794',
      600: '#52747f',
      700: '#446069',
      800: '#354c53',
      900: '#27383d',
      950: '#192427',
    },
  },
  semantic: {
    // brand olive scale
    primary: {
      50: '#fbfbf9',
      100: '#e2e0d2',
      200: '#c6c4a8',
      300: '#b4b18c',
      400: '#a7a478',
      500: '#a09c6d',
      600: '#8c885b',
      700: '#73704b',
      800: '#5b583b',
      900: '#42402b',
      950: '#2a291b',
    },
    focusRing: {
      width: '2.5px',
      style: 'solid',
      color: '{primary.color}',
      offset: '2px',
    },
    formField: {
      paddingX: '0.875rem',
      paddingY: '0.625rem',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.700}',
          contrastColor: '#fffaf0',
          hoverColor: '#5c5a3b',
          activeColor: '{primary.900}',
        },
        highlight: {
          background: '#e5e2c9',
          focusBackground: '#dad6b8',
          color: '#5c5a3b',
          focusColor: '#42402b',
        },
        // warm neutrals: 50 surface, 100 sunken, 200 border, 500 strong border, 600 muted ink, 950 ink
        surface: {
          0: '#ffffff',
          50: '#fffaf0',
          100: '#f5ecdc',
          200: '#ddd0b9',
          300: '#c9bca3',
          400: '#a89a82',
          500: '#857760',
          600: '#6b5e4e',
          700: '#564a3c',
          800: '#40352a',
          900: '#30261b',
          950: '#261a10',
        },
        mask: {
          background: 'rgba(20, 14, 8, 0.5)',
        },
        formField: {
          background: '{surface.50}',
          disabledBackground: '{surface.100}',
          borderColor: '{surface.500}',
          hoverBorderColor: '{surface.600}',
          color: '{surface.950}',
          placeholderColor: '{surface.600}',
          iconColor: '{surface.600}',
          shadow: 'none',
        },
        text: {
          color: '{surface.950}',
          hoverColor: '{surface.950}',
          mutedColor: '{surface.600}',
          hoverMutedColor: '{surface.700}',
        },
        content: {
          background: '{surface.50}',
          hoverBackground: '{surface.100}',
          borderColor: '{surface.200}',
        },
        overlay: {
          select: { background: '{surface.50}', borderColor: '{surface.200}' },
          popover: { background: '{surface.50}', borderColor: '{surface.200}' },
          modal: { background: '{surface.50}', borderColor: '{surface.200}' },
        },
      },
      dark: {
        primary: {
          color: '#cbc795',
          contrastColor: '#1c1b10',
          hoverColor: '#dedaa9',
          activeColor: '#e8e5bd',
        },
        highlight: {
          background: '#33321f',
          focusBackground: '#3f3e27',
          color: '#cbc795',
          focusColor: '#dedaa9',
        },
        // true neutrals: 950 ground, 900 surface, 800 sunken, 700 border, 500 strong border, 400 muted ink, 50 ink
        surface: {
          0: '#ffffff',
          50: '#f3efe6',
          100: '#e3ded3',
          200: '#cbc5b9',
          300: '#b8b1a5',
          400: '#a59e92',
          500: '#757368',
          600: '#4f4d47',
          700: '#32312d',
          800: '#252522',
          900: '#1a1a18',
          950: '#0e0e0d',
        },
        mask: {
          background: 'rgba(0, 0, 0, 0.6)',
        },
        formField: {
          background: '{surface.900}',
          disabledBackground: '{surface.800}',
          borderColor: '{surface.500}',
          hoverBorderColor: '{surface.400}',
          color: '{surface.50}',
          placeholderColor: '{surface.400}',
          iconColor: '{surface.400}',
          shadow: 'none',
        },
        text: {
          color: '{surface.50}',
          hoverColor: '{surface.50}',
          mutedColor: '{surface.400}',
          hoverMutedColor: '{surface.300}',
        },
        content: {
          background: '{surface.900}',
          hoverBackground: '{surface.800}',
          borderColor: '{surface.700}',
        },
        overlay: {
          select: { background: '{surface.900}', borderColor: '{surface.700}' },
          popover: { background: '{surface.900}', borderColor: '{surface.700}' },
          modal: { background: '{surface.900}', borderColor: '{surface.700}' },
        },
      },
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '12px',
        label: {
          fontWeight: '800',
        },
      },
      colorScheme: {
        light: {
          root: {
            secondary: {
              color: '{surface.700}',
              hoverColor: '{surface.800}',
              activeColor: '{surface.900}',
            },
            success: {
              background: '{green.800}',
              hoverBackground: '{green.900}',
              activeBackground: '{green.950}',
              borderColor: '{green.800}',
              hoverBorderColor: '{green.900}',
              activeBorderColor: '{green.950}',
            },
            warn: {
              background: '{orange.500}',
              hoverBackground: '{orange.600}',
              activeBackground: '{orange.700}',
              borderColor: '{orange.500}',
              hoverBorderColor: '{orange.600}',
              activeBorderColor: '{orange.700}',
              color: '#261a10',
              hoverColor: '#261a10',
              activeColor: '#261a10',
            },
            danger: {
              background: '{red.700}',
              hoverBackground: '{red.800}',
              activeBackground: '{red.900}',
              borderColor: '{red.700}',
              hoverBorderColor: '{red.800}',
              activeBorderColor: '{red.900}',
            },
            info: {
              background: '{sky.700}',
              hoverBackground: '{sky.800}',
              activeBackground: '{sky.900}',
              borderColor: '{sky.700}',
              hoverBorderColor: '{sky.800}',
              activeBorderColor: '{sky.900}',
            },
          },
          outlined: {
            secondary: { color: '{surface.600}' },
            success: { color: '{green.800}' },
            warn: { color: '{orange.800}' },
            danger: { color: '{red.700}' },
            info: { color: '{sky.700}' },
          },
          text: {
            secondary: { color: '{surface.600}' },
            success: { color: '{green.800}' },
            warn: { color: '{orange.800}' },
            danger: { color: '{red.700}' },
            info: { color: '{sky.700}' },
          },
        },
      },
    },
    datatable: {
      headerCell: {
        background: '{content.background}',
        borderColor: '{content.border.color}',
        color: '{text.muted.color}',
        selectedBackground: '{highlight.background}',
        selectedColor: '{highlight.color}',
      },
      columnTitle: {
        fontWeight: '700',
      },
      row: {
        hoverBackground: '{content.hover.background}',
      },
      bodyCell: {
        borderColor: '{content.border.color}',
      },
    },
    dialog: {
      root: {
        borderRadius: '20px',
      },
    },
    menu: {
      root: {
        borderRadius: '14px',
      },
    },
    select: {
      overlay: {
        borderRadius: '14px',
      },
    },
    tag: {
      root: {
        fontWeight: '800',
        borderRadius: '9999px',
      },
      colorScheme: {
        light: {
          primary: { color: '{primary.800}' },
          success: { color: '{green.800}' },
          warn: { color: '{orange.900}' },
          danger: { color: '{red.800}' },
        },
      },
    },
    message: {
      colorScheme: {
        light: {
          success: {
            color: '{green.800}',
            outlined: { color: '{green.800}' },
            simple: { color: '{green.800}' },
          },
          warn: {
            color: '{yellow.800}',
            outlined: { color: '{yellow.800}' },
            simple: { color: '{yellow.800}' },
          },
          error: {
            color: '{red.700}',
            outlined: { color: '{red.700}' },
            simple: { color: '{red.700}' },
          },
          secondary: {
            color: '{surface.700}',
            outlined: { color: '{surface.700}' },
            simple: { color: '{surface.700}' },
          },
        },
      },
    },
    toast: {
      root: {
        borderRadius: '1rem',
      },
      colorScheme: {
        light: {
          success: { color: '{green.800}' },
          warn: { color: '{yellow.800}' },
          error: { color: '{red.700}' },
          secondary: { color: '{surface.700}' },
        },
      },
    },
  },
});
